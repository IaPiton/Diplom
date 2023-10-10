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
    public SearchService() throws IOException {
    }

    public Object search(String query, String url, int offset, int limit) {
        Site site = siteRepository.findByUrl(url);
        searchLemmaMap = lemmanisator.textToLemma(query);
        List<String> textLemmaList = new ArrayList<>(searchLemmaMap.keySet());
        lemmaRepository.flush();
        List<Lemma> foundLemmaList = lemmaRepository.findByLemmaInAndSiteByLemmaOrderByFrequency(textLemmaList, site);
        List<Integer> lemmaId = new ArrayList<>();
        for (Lemma lemma :foundLemmaList){
            System.out.println(lemma.getId());
            lemmaId.add(lemma.getId());
        }
        List<Page> pageList = pageRepository.pageInIndex(lemmaId);
        List<Integer> pageId = new ArrayList<>();
        for (Page page : pageList){
            System.out.println(page.getId());
            pageId.add(page.getId());
        }
        List<Indexes> indexesList = indexesRepository.findByPageAndLemmas(lemmaId, pageId);
        for (Indexes indexes : indexesList){
            System.out.println(indexes.getId());
        }
        Map<Page, Float> relevanceMap = new HashMap<>();
        int i = 0;
        while (i < pageList.size()) {
            Page page = pageList.get(i);
            float relevance = 0;
            int j = 0;
            while (j < indexesList.size()) {
                Indexes indexes = indexesList.get(j);
                if(indexes.getPageByIndex() == page)
                {
                    relevance += indexes.getRankLemma();
                }
                j++;
            }
            relevanceMap.put(page, relevance);
           i++;
        }

        Map<Page, Float> allRelevanceMap = new HashMap<>();
        relevanceMap.keySet().forEach(page -> {
            float relevance = relevanceMap.get(page) / Collections.max(relevanceMap.values());
            allRelevanceMap.put(page, relevance);
        });
        List<Map.Entry<Page, Float>> sortList = new ArrayList<>(allRelevanceMap.entrySet());
        sortList.sort(Map.Entry.comparingByValue(Comparator.reverseOrder()));
        Map<Page, Float> map = new ConcurrentHashMap<>();
        Map.Entry<Page, Float> pageFloatEntry;
        int y = 0;
        while (y < sortList.size()) {
            pageFloatEntry = sortList.get(y);
            map.putIfAbsent(pageFloatEntry.getKey(), pageFloatEntry.getValue());
            y++;
        }
List<SearchDto> searchDtoList = new ArrayList<>();
        StringBuilder titleStringBuilder = new StringBuilder();
        for (Page page : map.keySet()){
            String urlPage = page.getPath();
            String content = page.getContent();
            Site pageSite = page.getSiteByPage();
            String sites = pageSite.getUrl();
            String siteName = pageSite.getName();
            String title = cleanCodeForm(content, "title");
            String body = cleanCodeForm(content, "body");
            titleStringBuilder.append(title).append(body);
            int pageValue = pageList.size();
            List<Integer> lemmaIndex = new ArrayList<>();
            int x = 0;
            while (i < textLemmaList.size()){
                String lemma =textLemmaList.get(x);
                lemmaIndex.addAll(lemmanisator)
            }
        }
        return null;
    }
    public String cleanCodeForm(String text, String element)
    {
        Document doc = Jsoup.parse(text);
        Elements elements = doc.select(element);
        String html = elements.stream().map(Element::html).collect(Collectors.joining());
        return Jsoup.parse(html).text();
    }
}
