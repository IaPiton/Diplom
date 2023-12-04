package searchengine.services;

import lombok.Getter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import searchengine.dto.ResultDto;
import searchengine.dto.SearchDto;
import searchengine.model.*;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.DateBaseService;
import searchengine.utils.Lemmanisator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class SearchService {
    Lemmanisator lemmanisator = new Lemmanisator();
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private DateBaseService dateBaseService;
    @Autowired
    private IndexesRepository indexesRepository;
    @Autowired
    private PageRepository pageRepository;
    private int pageNumber = -1;

    public SearchService() throws IOException {
    }

    public List<SearchDto> fullSearch(String query, int offset, int limit) {
        List<Site> siteList = siteRepository.findAll();
        List<Integer> siteId = dateBaseService.siteId(siteList);
        return createQueryList(siteId, query, offset, limit);
    }

    public List<SearchDto> search(String query, List<Site> siteList, int offset, int limit) {
        List<Integer> siteId = dateBaseService.siteId(siteList);
        return createQueryList(siteId, query, offset, limit);
    }

    public List<SearchDto> createQueryList(List<Integer> siteId, String query, int offset, int limit) {
        DateBaseService.setCountPage(new AtomicInteger(0));
        List<SearchDto> searchDtoList = new ArrayList<>();
        List<String> queryList = textLemma(query);
        if (queryList.isEmpty()) {
            return searchDtoList;
        }
        if (queryList.size() > 1) {
            return sortedMinLemma(siteId, queryList, offset, limit);
        }
        String lemma = queryList.get(0);
        List<Integer> pageId = pageRepository.idByLemma(lemma, siteId);
        return createSearchList(pageId, lemma, queryList, offset, limit);
    }

    public List<SearchDto> sortedMinLemma(List<Integer> siteId, List<String> queryList, int offset, int limit) {
        List<SearchDto> searchDtoList = new ArrayList<>();
        LinkedHashMap<String, Integer> lemmaByFrequency = new LinkedHashMap<>();
        for (String lemma : queryList) {
            Integer frequency = lemmaRepository.frequencyLemma(lemma, siteId);
            if (frequency != null) {
                lemmaByFrequency.put(lemma, frequency);
            }
        }
        if (lemmaByFrequency.isEmpty()) {
            return searchDtoList;
        }
        lemmaByFrequency = sortedLemmaMap(lemmaByFrequency);
        String minLemma = lemmaByFrequency.entrySet().stream().findFirst().get().getKey();
        return coincidenceLemmaToPage(siteId, lemmaByFrequency, minLemma, queryList, offset, limit);
    }

    public List<SearchDto> coincidenceLemmaToPage(List<Integer> siteId,
                                                  LinkedHashMap<String, Integer> lemmaByFrequency, String minLemma,
                                                  List<String> queryList, int offset, int limit) {
        HashMap<Page, Integer> coincidenceMap = new HashMap<>();
        for (String lemma : lemmaByFrequency.keySet()) {
            List<Indexes> indexesList = indexesRepository.findIndexByLemmas(lemma, siteId);
            for (Indexes indexes : indexesList) {
                if (coincidenceMap.containsKey(indexes.getPageByIndex())) {
                    coincidenceMap.put(indexes.getPageByIndex(), coincidenceMap.get(indexes.getPageByIndex()) + 1);
                } else {
                    coincidenceMap.put(indexes.getPageByIndex(), 1);
                }
            }
        }
        List<Integer> pageId = new ArrayList<>();
        for (Page page : coincidenceMap.keySet()) {
            if (coincidenceMap.get(page) == queryList.size()) {
                pageId.add(page.getId());
            }
        }
        return createSearchList(pageId, minLemma, queryList, offset, limit);
    }


    public List<SearchDto> createSearchList(List<Integer> pageId, String minLemma,
                                            List<String> queryList, int offset, int limit) {
        DateBaseService.setCountPage(new AtomicInteger(pageId.size()));
        pageNumber = offset == 0 ? pageNumber = 0 : pageNumber++;
        Pageable pageable = PageRequest.of(pageNumber, limit, Sort.by("rank_lemma").descending());
        List<Indexes> indexesList = indexesRepository.findIndexByLemmaAndPage(pageId, minLemma, pageable);
        List<Page> pageList = new ArrayList<>();
        for (Indexes indexes : indexesList) {
            pageList.add(indexes.getPageByIndex());
        }
        return relevanceMap(pageList, indexesList, queryList);
    }

    public List<SearchDto> relevanceMap(List<Page> pageList, List<Indexes> indexesList, List<String> textLemmaList) {
        HashMap<Page, Float> relevanceMap = new HashMap<>();
        int i = 0;
        while (i < pageList.size()) {
            Page page = pageList.get(i);
            float relevance = 0;
            int j = 0;
            while (j < indexesList.size()) {
                Indexes indexes = indexesList.get(j);
                if (indexes.getPageByIndex() == page) {
                    relevance += indexes.getRankLemma();
                }
                j++;
            }
            relevanceMap.put(page, relevance);
            i++;
        }
        return relativeRelevanceMap(relevanceMap, textLemmaList);
    }

    public List<SearchDto> relativeRelevanceMap(HashMap<Page, Float> relevanceMap, List<String> textLemmaList) {
        Map<Page, Float> allRelevanceMap = new HashMap<>();
        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });
        LinkedHashMap<Page, Float> relativeRelevanceMap = sortedPageMap(allRelevanceMap);
        return createSearchPage(relativeRelevanceMap, textLemmaList);
    }

    public List<SearchDto> createSearchPage(LinkedHashMap<Page, Float> relativeRelevanceMap, List<String> textLemmaList) {
        int limitTitle = 100;
        List<SearchDto> searchDtoList = new ArrayList<>();
        StringBuilder bodyStringBuilder = new StringBuilder();
        List<Page> pages = new ArrayList<>();
        for (Page page : relativeRelevanceMap.keySet()) {
            pages.add(page);
        }
        for (Page page : pages) {
            String urlPage = page.getPath();
            String content = page.getContent();
            Site pageSite = page.getSiteByPage();
            String sites = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = cleanCodeForm(content, "title");
            title = title.length() > limitTitle ? title.substring(0, limitTitle) : title;
            String body = cleanCodeForm(content, "body");
            bodyStringBuilder.append(body);
            Float pageValue = relativeRelevanceMap.get(page);
            StringBuilder snippetBuilder = new StringBuilder();
            List<Integer> lemmaIndex = findLemmaIndex(body, textLemmaList);
            List<String> wordList = getWordsFromContent(body, lemmaIndex);
            int y = 0;
            while (y < wordList.size()) {
                snippetBuilder.append(wordList.get(y)).append("...  ");
                if (y > 3) {
                    break;
                }
                y++;
            }
            lemmaIndex.clear();
            searchDtoList.add(new SearchDto(sites, siteName, urlPage, title, snippetBuilder.toString(), pageValue));
        }
        return searchDtoList;
    }

    public static String cleanCodeForm(String text, String element) {
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }

    public List<String> textLemma(String query) {
        HashMap<String, Integer> searchLemmaMap = lemmanisator.textToLemma(query);
        List<String> textLemmaList = new ArrayList<>(searchLemmaMap.keySet());
        return textLemmaList;
    }

    public List<Integer> findLemmaIndex(String body, List<String> textLemmaList) {
        List<Integer> lemmaIndex = new ArrayList<>();
        int i = 0;
        while (i < textLemmaList.size()) {
            String lemma = textLemmaList.get(i);
            lemmaIndex.addAll(lemmanisator.findLemmaIndexInWord(body, lemma));
            i++;
        }
        Collections.sort(lemmaIndex);
        return lemmaIndex;
    }

    public List<String> getWordsFromContent(String content, List<Integer> lemmaIndex) {
        List<String> wordList = new ArrayList<>();
        int i = 0;
        int sizeSniper = 3;
        if (lemmaIndex.size() < sizeSniper) {
            sizeSniper = lemmaIndex.size();
        }
        while (wordList.size() < sizeSniper) {
            int start = lemmaIndex.get(i);
            int end = content.indexOf(" ", start);
            int next = i + 1;
            while (next < lemmaIndex.size() && lemmaIndex.size() >= next) {
                if (lemmaIndex.get(next) - end < 25) {
                    end = content.indexOf(" ", lemmaIndex.get(next));
                    sizeSniper = sizeSniper - 1;
                }
                next += 1;
            }
            String word = content.substring(start, end);
            word = word.replaceAll("\\(", "").replaceAll("\\)", "");
            int sizeTextSniper = 100;
            if (sizeSniper == 2) {
                sizeTextSniper = 200;
            } else if (sizeSniper == 1) {
                sizeTextSniper = 300;
            }
            if ((end + sizeTextSniper) > content.length()) {
                sizeTextSniper = content.length() - end;
            }
            int endSniper = end + sizeTextSniper;
            String text = content.substring(start, endSniper).replaceAll(word, "<b>".concat(word).concat("</b>"));
            wordList.add(text);
            i++;
        }
        return wordList;
    }

    public LinkedHashMap<Page, Float> sortedPageMap(Map<Page, Float> allRelevanceMap) {
        List<Map.Entry<Page, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        LinkedHashMap<Page, Float> relativeRelevanceMap = new LinkedHashMap<>();
        Map.Entry<Page, Float> pageFloatEntry;
        int y = 0;
        while (y < sortList.size()) {
            pageFloatEntry = sortList.get(y);
            relativeRelevanceMap.putIfAbsent(pageFloatEntry.getKey(), pageFloatEntry.getValue());
            y++;
        }
        return relativeRelevanceMap;
    }

    public LinkedHashMap<String, Integer> sortedLemmaMap(Map<String, Integer> allLemmaMap) {
        List<Map.Entry<String, Integer>> sortList = new ArrayList<>(allLemmaMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue());
        LinkedHashMap<String, Integer> sortedLemmaMap = new LinkedHashMap<>();
        Map.Entry<String, Integer> pageFloatEntry;
        int y = 0;
        while (y < sortList.size()) {
            pageFloatEntry = sortList.get(y);
            sortedLemmaMap.putIfAbsent(pageFloatEntry.getKey(), pageFloatEntry.getValue());
            y++;
        }
        return sortedLemmaMap;
    }
}
