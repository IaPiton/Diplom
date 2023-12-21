package searchengine.utils;

import lombok.Data;


import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import searchengine.config.SiteConfig;

import searchengine.model.*;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.services.SearchService;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@Data
public class DateBaseService {

    @Autowired
    private SiteConfig sites;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private IndexesRepository indexesRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Setter
    @Getter
    private static AtomicInteger countPage = new AtomicInteger();
    @Setter
    @Getter
    private static AtomicBoolean indexingRun = new AtomicBoolean(false);
    @Setter
    @Getter
    private static AtomicBoolean indexingStop = new AtomicBoolean(false);
    @Setter
    @Getter
    private static AtomicInteger countSite = new AtomicInteger(0);
    @Setter
    @Getter
    private static AtomicInteger indexedSite = new AtomicInteger(0);

    public void deleteAll(){
        indexesRepository.deleteAll();
        lemmaRepository.deleteAll();
        pageRepository.deleteAll();
        siteRepository.deleteAll();
    }

    public Site findSiteByName(Site site) {
        site = siteRepository.findByName(site.getName());
        return site;
    }

    public Site updateSite(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
        return site;
    }

    public Page addPageToDateBase(String path, int code, String content, Site site) {
        Page page = new Page();
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        page.setSiteByPage(site);
        pageRepository.saveAndFlush(page);
        return page;
    }

    public void addEntitiesToDateBase(Document doc, String url, int code, Site site) throws IOException {
        String content = "Page not found";
        if (!(doc == null)) {
            content = doc.html();
        } else {
            updateLastError(site, url + " - " + "Страница пустая");
        }

        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        Page page = addPageToDateBase(path, code, content, site);
        if (code == 200) {
            content = SearchService.cleanCodeForm(content, "body");
            createIndexAndLemma(content, page, site);
        }
    }

    public void createIndexAndLemma(String content, Page page, Site site) throws IOException {
        Lemmanisator lemmanisator = new Lemmanisator();
        HashMap<String, Integer> lemma = lemmanisator.textToLemma(content);
        addLemmaToDateBase(lemma, site, page);
    }

    public void addLemmaToDateBase(HashMap<String, Integer> lemmaMap, Site site, Page page) {
        Map<Integer, Integer> indexMap = new HashMap<>();
        for (String lemmas : lemmaMap.keySet()) {
            Lemma lemma = new Lemma();
            if (!lemmaRepository.existsByLemmaAndSiteByLemma(lemmas, site)) {
                lemma.setLemma(lemmas);
                lemma.setSiteByLemma(site);
                lemma.setFrequency(1);
            } else {
                lemma = lemmaRepository.findFirstByLemmaAndSiteByLemma(lemmas, site);
                lemma.setFrequency(lemma.getFrequency() + 1);
            }
            lemmaRepository.saveAndFlush(lemma);
            indexMap.put(lemma.getId(), lemmaMap.get(lemmas));
        }
        indexAddToDB(indexMap, page, site);
    }

    public void indexAddToDB(Map<Integer, Integer> lemmaMap, Page page, Site site) {
        for (Integer lemmaIndex : lemmaMap.keySet()) {
            Indexes index = new Indexes();
            Lemma lemma = lemmaRepository.findById(lemmaIndex).get();
            index.setPageByIndex(page);
            index.setLemmaByIndex(lemma);
            index.setRankLemma(lemmaMap.get(lemmaIndex));
            indexesRepository.saveAndFlush(index);
        }
        updateSite(site, Status.INDEXING);
    }

    public void updateLastError(Site site, String errorMessage) {
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
    }

     public List<Integer> siteId(List<Site> site) {
        List<Integer> siteId = new ArrayList<>();
        for (Site sites : site) {
            siteId.add(sites.getId());
        }
        return siteId;
    }

    public void lemmaByIndexesUpdate(List<Indexes> indexesList) {
        for(Indexes indexes :indexesList){
           Lemma lemma = lemmaRepository.findLemmaByIndexesById(indexes);
           if(lemma.getFrequency() == 1){
               indexesRepository.delete(indexes);
               lemmaRepository.delete(lemma);
           }else{
               lemma.setFrequency(lemma.getFrequency() - 1);
               lemmaRepository.saveAndFlush(lemma);
               indexesRepository.delete(indexes);
           }
        }
    }
}