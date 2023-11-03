package searchengine.services;

import jakarta.persistence.EntityManager;
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

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
    private  AtomicBoolean indexingRun = new AtomicBoolean();
    private  AtomicBoolean indexingStop = new AtomicBoolean();

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

    @Transactional
    public void deletePathByPage(Integer idPage) {
        pageRepository.deletePathByPage(idPage);
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
        List<Integer> lemmaIdByPat = indexesRepository.findLemmaByPath(idPath);
        return lemmaIdByPat;
    }
    public List<Integer> siteId (List<Site> site){
        List<Integer> siteId = new ArrayList<>();
        for (Site sites : site){
            siteId.add(sites.getId());
        }
        return siteId;
    }
    public List<Page> pageListByLemma (List<Lemma> lemmaList){
        List<Page> pageList = new ArrayList<>();
        List<Integer> lemmaId = lemmaId(lemmaList);
        pageList = pageRepository.pageInIndex(lemmaId);
        return pageList;
    }
    public List<Integer> lemmaId (List<Lemma> lemmaList){
        List<Integer> lemmaId = new ArrayList<>();
        for (Lemma lemma : lemmaList){
            lemmaId.add(lemma.getId());
        }
        return lemmaId;
    }

    public List<Indexes> indexesListByLemmaByPage (List<Lemma> lemmaList, List<Page> pageList){
        List<Indexes> indexesList = new ArrayList<>();
        List<Integer> lemmaId = lemmaId(lemmaList);
        List<Integer> pageId = new ArrayList<>();
        for (Page page : pageList){
            pageId.add(page.getId());
        }
        indexesList = indexesRepository.findByPageAndLemmas(lemmaId, pageId);
        return indexesList;
    }

}
