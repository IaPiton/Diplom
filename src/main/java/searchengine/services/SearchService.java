package searchengine.services;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.dto.SearchDto;
import searchengine.model.Indexes;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import utils.Lemmanisator;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SearchService {
    Lemmanisator lemmanisator = new Lemmanisator();
    private SiteConfig siteConfig;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private DateBaseService dateBaseService;
    @Autowired
    private IndexesRepository indexesRepository;
    @Autowired
    private PageRepository pageRepository;
    public SearchService() throws IOException {
    }

    public List<SearchDto> fullSearch(String query, int offset, int limit) {
        List<Site> siteList = siteRepository.findAll();
        List<Integer> siteId = dateBaseService.siteId(siteList);
        return createSearchPage(siteId, query, offset, limit);
    }

    public List<SearchDto> search(String query, String url, int offset, int limit) {
        List<Site> siteList = siteRepository.findByUrl(url);
        List<Integer> siteId = dateBaseService.siteId(siteList);
        return createSearchPage(siteId, query, offset, limit);
    }

    public List<SearchDto> createSearchPage(List<Integer> siteId, String query, int offset, int limit) {
        List<String> textLemmaList = textLemma(query);
        Pageable firstPageWithLimitElements = PageRequest.of(offset, limit, Sort.by("rank_lemma" ).descending());
        List<Indexes> indexesListTest = indexesRepository.findIndexByLemma(textLemmaList, siteId, firstPageWithLimitElements);
        List<Page> pageList = new ArrayList<>();
        for (Indexes indexes : indexesListTest){
            pageList.add(indexes.getPageByIndex());
        }
       HashMap<Page, Float> relevanceMap = relevanceMap(pageList, indexesListTest);
        LinkedHashMap<Page, Float> relativeRelevanceMap = relativeRelevanceMap(relevanceMap);
       List<SearchDto> searchDtoList = createSearchDtoList(relativeRelevanceMap, textLemmaList);

        return searchDtoList;
    }

    public List<SearchDto> createSearchDtoList (LinkedHashMap<Page, Float> relativeRelevanceMap, List<String> textLemmaList){
        List<SearchDto> searchDtoList = new ArrayList<>();
        StringBuilder bodyStringBuilder = new StringBuilder();
        for (Page page : relativeRelevanceMap.keySet()) {
            String urlPage = page.getPath();
            String content = page.getContent();
            Site pageSite = page.getSiteByPage();
            String sites = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = cleanCodeForm(content, "title");
            String body = cleanCodeForm(content, "body");
            bodyStringBuilder.append(body);
            Float pageValue = relativeRelevanceMap.get(page);
            StringBuilder snippetBuilder = new StringBuilder();
            List<Integer> lemmaIndex = findLemmaIndex(body,textLemmaList);
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

    public String cleanCodeForm(String text, String element) {
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }

    public List<String> textLemma(String query) {
        HashMap<String, Integer>searchLemmaMap = lemmanisator.textToLemma(query);
        List<String> textLemmaList = new ArrayList<>(searchLemmaMap.keySet());
        return textLemmaList;
    }

    public HashMap<Page, Float> relevanceMap(List<Page> pageList, List<Indexes> indexesList) {
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
        return relevanceMap;
    }

    public LinkedHashMap<Page, Float> relativeRelevanceMap(HashMap<Page, Float> relevanceMap) {
        Map<Page, Float> allRelevanceMap = new HashMap<>();
        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });
        List<Map.Entry<Page, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        LinkedHashMap<Page, Float> relativeRelevanceMap = new LinkedHashMap<>();
        Map.Entry<Page, Float> pageFloatEntry;
        int y = 0;
        while (y <sortList.size()) {
            pageFloatEntry = sortList.get(y);
            relativeRelevanceMap.putIfAbsent(pageFloatEntry.getKey(), pageFloatEntry.getValue());
            y++;
        }
        return relativeRelevanceMap;
    }
    public List<Integer> findLemmaIndex (String body,  List<String> textLemmaList){
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
            int x = content.length();
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
}
