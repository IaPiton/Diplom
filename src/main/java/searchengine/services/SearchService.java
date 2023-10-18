package searchengine.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConfig;
import searchengine.dto.ResultDto;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class SearchService {
    HashMap<String, Integer> searchLemmaMap;
    Lemmanisator lemmanisator = new Lemmanisator();

    private int resultsNumber;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexesRepository indexesRepository;
    @Autowired
    private DateBaseService dateBaseService;

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
        searchLemmaMap = lemmanisator.textToLemma(query);
        List<String> textLemmaList = new ArrayList<>(searchLemmaMap.keySet());
        List<Lemma> foundLemmaList = lemmaRepository.findByLemmaAndSiteOrderByFrequency(textLemmaList, siteId);
        List<Page> pageList = dateBaseService.pageListByLemma(foundLemmaList);
        List<Indexes> indexesList = dateBaseService.indexesListByLemmaByPage(foundLemmaList, pageList);
        HashMap<Page, Float> relevanceMap = relevanceMap(pageList, indexesList);
        LinkedHashMap<Page, Float> relativeRelevanceMap = relativeRelevanceMap(relevanceMap, offset, limit);
        List<SearchDto> searchDtoList = createSearchDtoList(relativeRelevanceMap, textLemmaList);
        return searchDtoList;
    }

    public List<SearchDto> createSearchDtoList (LinkedHashMap<Page, Float> relativeRelevanceMap, List<String> textLemmaList){
        List<SearchDto> searchDtoList = new ArrayList<>();
        StringBuilder titleStringBuilder = new StringBuilder();
        for (Page page : relativeRelevanceMap.keySet()) {
            String urlPage = page.getPath();
            String content = page.getContent();
            Site pageSite = page.getSiteByPage();
            String sites = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = cleanCodeForm(content, "title");
            String body = cleanCodeForm(content, "body");
            titleStringBuilder.append(title).append(body);
            int pageValue = relativeRelevanceMap.size();
            StringBuilder snippetBuilder = new StringBuilder();
            snippetBuilder.append("123");
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
        searchLemmaMap = lemmanisator.textToLemma(query);
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

    public LinkedHashMap<Page, Float> relativeRelevanceMap(HashMap<Page, Float> relevanceMap, int offset, int limit) {
        Map<Page, Float> allRelevanceMap = new HashMap<>();
        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });
        List<Map.Entry<Page, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        LinkedHashMap<Page, Float> relativeRelevanceMap = new LinkedHashMap<>();
        Map.Entry<Page, Float> pageFloatEntry;
        int y = offset;
        if (sortList.size() < limit){
            limit = sortList.size();
        }
        while (y < limit) {
            pageFloatEntry = sortList.get(y);
            relativeRelevanceMap.putIfAbsent(pageFloatEntry.getKey(), pageFloatEntry.getValue());
            y++;
        }
        return relativeRelevanceMap;
    }

}
