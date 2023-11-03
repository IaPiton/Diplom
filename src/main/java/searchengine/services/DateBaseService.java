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

    @Transactional(readOnly = true)
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
                addLemmaToDateBase(lemmas, lemma.get(lemmas),  site, page);
            }
        }
        updateSite(site, Status.INDEXING);
    }

    public  void addLemmaToDateBase(String lemmas, int rank, Site site, Page page) {
    Lemma lemma = new Lemma();
        if (!lemmaRepository.existsByLemmaAndSiteByLemma(lemmas, site)) {
            lemma.setSiteByLemma(site);
            lemma.setLemma(lemmas);
            lemma.setFrequency(1);
            lemmaRepository.saveAndFlush(lemma);
            indexAddToDB(lemmas, rank, page, site);
            return;
        }
        updateLemma(lemmas, rank, page, site);

    }


    public synchronized void updateLemma (String lemmas, int rank, Page page, Site site){
        lemmaRepository.updateLemmaFrequency(site, lemmas);
        indexAddToDB(lemmas, rank, page, site);
    }
    public void indexAddToDB(String lemmas, int rank, Page page, Site site) {
            Indexes index = new Indexes();
            index.setPageByIndex(page);
            index.setLemmaByIndex(lemmaRepository.idToLemma(lemmas, site.getId()));
            index.setRankLemma(rank);
            indexesRepository.saveAndFlush(index);
    }


    @Transactional()
    public Site updateLastError(Site site, String errorMessage) {
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        return siteRepository.save(site);
    }
    @Transactional
    public List<Integer> findPathByPage(String path) {
        List<Integer> result = pageRepository.findPathByPage(path);
        return result;
    }


    public List<Integer> lemmaIdByPath(Integer idPath) {
        List<Integer> lemmaIdByPath = indexesRepository.findLemmaByPath(idPath);
        return lemmaIdByPath;
    }
    public List<Integer> siteId (List<Site> site){
        List<Integer> siteId = new ArrayList<>();
        for (Site sites : site){
            siteId.add(sites.getId());
        }
        return siteId;
    }

    public List<Integer> lemmaId (List<Lemma> lemmaList){
        List<Integer> lemmaId = new ArrayList<>();
        for (Lemma lemma : lemmaList){
            lemmaId.add(lemma.getId());
        }
        return lemmaId;
    }

}
