package searchengine.utils;

import lombok.Data;


import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;
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
import java.util.concurrent.locks.ReentrantLock;

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


    @Transactional
    public Site findSiteByName(Site site) {
        site = siteRepository.findByName(site.getName());
        return site;
    }

    @Transactional
    public Site updateSite(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
        return site;
    }

    @Transactional
    public Page addPageToDateBase(String path, int code, String content, Site site, int pageId) {
        Page page = new Page();
        if (!(pageId == 0)) {
            page.setId(pageId);
        }
        page.setPath(path);
        page.setCode(code);
        page.setContent(content);
        page.setSiteByPage(site);
        pageRepository.saveAndFlush(page);
        return page;
    }

    public void addEntitiesToDateBase(Document doc, String url, int code, Site site, int pageId) throws IOException {
        String content = "Page not found";
        if (!(doc == null)) {
            content = doc.html();
        } else {
            updateLastError(site, url + " - " + "Страница пустая");
        }


        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        Page page = addPageToDateBase(path, code, content, site, pageId);
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

    @Transactional
    public void addLemmaToDateBase(HashMap<String, Integer> lemmaMap, Site site, Page page) {
        Map<Integer, Integer> indexMap = new HashMap<>();
        for (String lemmas : lemmaMap.keySet()) {
            Lemma lemma = new Lemma();
            if (!lemmaRepository.existsByLemmaAndSiteByLemma(lemmas, site)) {
                lemma.setLemma(lemmas);
                lemma.setSiteByLemma(site);
                lemma.setFrequency(1);
                lemmaRepository.saveAndFlush(lemma);
                indexMap.put(lemma.getId(), lemmaMap.get(lemmas));
            } else {
                lemma = lemmaRepository.findFirstByLemmaAndSiteByLemma(lemmas, site);
                lemma.setFrequency(lemma.getFrequency() + 1);
                lemmaRepository.saveAndFlush(lemma);
                indexMap.put(lemma.getId(), lemmaMap.get(lemmas));
            }

        }
        indexAddToDB(indexMap, page, site);
    }


    public void indexAddToDB(Map<Integer, Integer> lemmaMap, Page page, Site site) {
        for (Integer lemmaIndex : lemmaMap.keySet()) {
            Indexes index = new Indexes();
            Lemma lemma = new Lemma();
            lemma = lemmaRepository.findById(lemmaIndex).get();
            index.setPageByIndex(page);
            index.setLemmaByIndex(lemma);
            index.setRankLemma(lemmaMap.get(lemmaIndex));
            indexesRepository.saveAndFlush(index);
        }
        updateSite(site, Status.INDEXING);
    }

    @Transactional
    public Site updateLastError(Site site, String errorMessage) {
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        siteRepository.saveAndFlush(site);
        return site;
    }

    public List<Integer> findPathByPage(String path) {
        List<Integer> result = pageRepository.findPathByPage(path);
        return result;
    }

    public List<Integer> lemmaIdByPath(Integer idPath) {
        List<Integer> lemmaIdByPath = indexesRepository.findLemmaByPath(idPath);
        return lemmaIdByPath;
    }

    public List<Integer> siteId(List<Site> site) {
        List<Integer> siteId = new ArrayList<>();
        for (Site sites : site) {
            siteId.add(sites.getId());
        }
        return siteId;
    }

}