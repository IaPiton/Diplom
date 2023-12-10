package searchengine.utils;


import jakarta.persistence.Version;
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
        return siteRepository.findByName(site.getName());
    }

    @Transactional
    public Site updateSite(Site site, Status status) {
        site.setStatus(status);
        site.setStatusTime(LocalDateTime.now());
        return siteRepository.saveAndFlush(site);
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

    public void addEntitiesToDateBase(Document doc, String url, int code, Site site, int pageId) throws IOException, InterruptedException {
        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        String content = "Page not found";
        if (!(doc == null)) {
            content = doc.html();
        }
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
@Version
    public void addLemmaToDateBase(HashMap<String, Integer> lemmaMap, Site site, Page page) {
        HashMap<Integer, Integer> lemmasMap = new HashMap<>();
        for (String lemmas : lemmaMap.keySet()) {
            Integer idLemma = null;
            Lemma lemma = new Lemma();
            idLemma = lemmaRepository.idToLemmaInt(lemmas, site.getId());
            lemma.setLemma(lemmas);
            lemma.setSiteByLemma(site);
            if (idLemma == null) {
                lemma.setFrequency(1);
            } else {
                lemma.setId(idLemma);
                lemma.setFrequency(lemmaRepository.frequencyById(idLemma) + 1);
            }
            lemmaRepository.saveAndFlush(lemma);
            if (idLemma == null) {
                idLemma = lemma.getId();
            }
            lemmasMap.put(idLemma, lemmaMap.get(lemmas));
        }
        indexAddToDB(lemmasMap, page, site);
    }

    public void indexAddToDB(HashMap<Integer, Integer> lemma, Page page, Site site) {
        List<Indexes> indexesList = new ArrayList<>();
        for (Integer idLemma : lemma.keySet()) {
            Indexes index = new Indexes();
            index.setPageByIndex(page);
            index.setLemmaByIndex(lemmaRepository.getReferenceById(idLemma));
            index.setRankLemma(lemma.get(idLemma));
            indexesList.add(index);
        }
        indexesRepository.saveAllAndFlush(indexesList);
        updateSite(site, Status.INDEXING);
    }

    @Transactional
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

    public List<Integer> siteId(List<Site> site) {
        List<Integer> siteId = new ArrayList<>();
        for (Site sites : site) {
            siteId.add(sites.getId());
        }
        return siteId;
    }

    public List<Integer> lemmaId(List<Lemma> lemmaList) {
        List<Integer> lemmaId = new ArrayList<>();
        for (Lemma lemma : lemmaList) {
            lemmaId.add(lemma.getId());
        }
        return lemmaId;
    }
}