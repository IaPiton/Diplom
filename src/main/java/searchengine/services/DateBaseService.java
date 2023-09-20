package searchengine.services;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConfig;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import utils.Lemmanisator;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@Getter
@Setter
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
    public Page addPageToDateBase(String path, int code, String content, Site site, int pageId){
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

        }

        updateSite(site, Status.INDEXING);
}
    @Transactional
public Lemma addLemmaToDateBase(HashMap<String, Integer> lemmaMap, Site site)
    {
        Lemma lemma = new Lemma();
        for(String lemmas : lemmaMap.keySet())
        {
            lemma.setSiteByLemma(site);
            lemma.setLemma(lemmas);
            lemma.setFrequency(lemmaMap.get(lemmas));
            lemmaRepository.saveAndFlush(lemma);
        }
        return lemma;
    }
    @Transactional
    public Site updateLastError(Site site, String errorMessage) {
        site.setLastError(errorMessage);
        site.setStatusTime(LocalDateTime.now());
        return siteRepository.save(site);
    }
}
