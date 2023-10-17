package searchengine.services;

import lombok.Data;


import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;

import searchengine.model.*;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import utils.Lemmanisator;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
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
    private volatile AtomicBoolean indexingRun = new AtomicBoolean();
    private volatile AtomicBoolean indexingStop = new AtomicBoolean();

    @Transactional(readOnly = true)
    public Site findSiteByName(Site site) {
        return siteRepository.findByName(site.getName());
    }

    @Transactional
    public Site updateSite(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        return siteRepository.saveAndFlush(site);
    }

    @Transactional
    public void deleteAllPages() {
        pageRepository.deleteAll();
    }

    @Transactional
    public void deleteAllLemma() {
        lemmaRepository.deleteAll();
    }

    @Transactional
    public void deleteAllIndexes() {
        indexesRepository.deleteAll();
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
        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        String content = "Page not found";
        if (!(doc == null)) {
            content = doc.html();
        }
        Page page = addPageToDateBase(path, code, content, site, pageId);
        if (code == 200) {
            Lemmanisator lemmanisator = new Lemmanisator();
            HashMap<String, Integer> lemma = lemmanisator.textToLemma(content);
            for (String lemmas : lemma.keySet()) {
                HashMap<String, Integer> lemmaMap = new HashMap<>();
                lemmaMap.put(lemmas, lemma.get(lemmas));
                HashMap<Integer, Float> mapId = addLemmaToDateBase(lemmaMap, site);
                indexAddToDB(page, mapId, site);
            }
        }
        updateSite(site, Status.INDEXING);
    }

    @Transactional
    public HashMap<Integer, Float> addLemmaToDateBase(HashMap<String, Integer> lemmaMap, Site site) {
        HashMap<Integer, Float> mapId = new HashMap<>();
        Lemma lemma = new Lemma();
        for (String lemmas : lemmaMap.keySet()) {
            lemma.setSiteByLemma(site);
            lemma.setLemma(lemmas);
            lemma.setFrequency(lemmaMap.get(lemmas));
            lemmaRepository.saveAndFlush(lemma);
            int idLemma = lemma.getId();
            mapId.put(idLemma, lemmaMap.get(lemmas).floatValue());
        }
        return mapId;
    }

    @Transactional
    public synchronized void indexAddToDB(Page page, HashMap<Integer, Float> idMap, Site site) {
        idMap.forEach((lemmaId, rank) -> {
            Indexes index = new Indexes();
            index.setPageByIndex(page);
            index.setLemmaByIndex(lemmaRepository.findByIdAndSiteByLemma(lemmaId, site));
            index.setRankLemma(rank);
            indexesRepository.save(index);
        });
    }

    @Transactional
    public Site updateLastError(Site site, String errorMessage) {
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        return siteRepository.save(site);
    }

    @Transactional
    public Integer findPathByPage(String path) {
        Integer result = pageRepository.findPathByPage(path);
        return result;
    }

    @Transactional
    public void deletePathByPage(String path) {
        pageRepository.deletePathByPage(path);
    }

    @Transactional
    public void deleteLemmaPathByPage(Integer id) {
        lemmaRepository.deleteLemmaPathByPage(id);
    }

    @Transactional
    public void deleteIndexPathByPage(Integer id) {
        indexesRepository.deleteIndexPathByPage(id);
    }

    public List<Integer> lemmaIdByPath(Integer idPath) {
        List<Integer> lemmaIdByPat = indexesRepository.lemmaIdByPath(idPath);
        return lemmaIdByPat;
    }
    public List<Integer> siteId (List<Site> site){
        List<Integer> siteId = new ArrayList<>();
        for (Site sites : site){
            siteId.add(sites.getId());
        }
        return siteId;
    }

}
