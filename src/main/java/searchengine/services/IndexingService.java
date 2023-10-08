package searchengine.services;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import searchengine.config.ParserConfig;
import searchengine.config.SiteConfig;

import searchengine.dto.ResponseTrue;
import searchengine.model.Site;
import searchengine.model.Status;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;


@Service
@Getter
@Setter
@Data
public class IndexingService {
    @Autowired
    private ParserConfig parserConfig;
    @Autowired
    private final DateBaseService dateBaseService;
    @Autowired
    private SiteConfig siteConfig;

    public ResponseTrue startIndexing() {
        ArrayList<Site> sites = siteConfig.getSites();
        dateBaseService.getIndexingRun().set(true);
        dateBaseService.getIndexingStop().set(false);
        dateBaseService.deleteAllIndexes();
        dateBaseService.deleteAllPages();
        dateBaseService.deleteAllLemma();
        for (Site site : sites) {
            CompletableFuture.runAsync(() -> {
                indexingSite(site);

            }, ForkJoinPool.commonPool());
        }
        return new ResponseTrue("true");
    }

    @Async
    public void indexingSite(Site siteForIndex) {
        Site siteInDateBase = updatingSite(siteForIndex);
        CopyOnWriteArraySet<String> linksSet = new CopyOnWriteArraySet<>();
        ParserLinksService parserLinks = new ParserLinksService(siteInDateBase.getUrl() + "/", linksSet, siteInDateBase);
        parseConfig(parserLinks);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parserLinks);
        indexingFinish(siteInDateBase);
    }

    public Object indexingPage(String url, Site siteForIndex) {
        dateBaseService.getIndexingRun().set(true);
        dateBaseService.getIndexingStop().set(false);
        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        if (!(dateBaseService.findPathByPage(path) == null)) {
            Integer idPath = dateBaseService.findPathByPage(path);
            List<Integer> lemmaId = dateBaseService.lemmaIdByPath(idPath);
            for (Integer id : lemmaId) {
                dateBaseService.deleteIndexPathByPage(id);
                dateBaseService.deleteLemmaPathByPage(id);
            }
            dateBaseService.deletePathByPage(path);
        }
        Site siteInDateBase = updatingSite(siteForIndex);
        CopyOnWriteArraySet<String> linksSet = new CopyOnWriteArraySet<>();
        ParserLinksService parserLinks = new ParserLinksService(url, linksSet, siteInDateBase);
        ForkJoinPool pool = new ForkJoinPool();
        parseConfig(parserLinks);
        pool.invoke(parserLinks);
        indexingFinish(siteInDateBase);
        return new ResponseTrue("true");
    }

    public Object stopIndexing() {
        dateBaseService.getIndexingRun().set(false);
        dateBaseService.getIndexingStop().set(true);
        return new ResponseTrue("true");
    }

    public void indexingFinish(Site siteInDateBase) {

        if (dateBaseService.getIndexingStop().get()) {
            siteInDateBase.setLastError("Индексация остановлена");
            dateBaseService.updateSite(siteInDateBase, Status.FAILED);
        } else {
            dateBaseService.updateSite(siteInDateBase, Status.INDEXED);
        }

        dateBaseService.getIndexingRun().set(false);
    }

    public void parseConfig(ParserLinksService parserLinks) {
        parserLinks.setParserConfig(parserConfig);
        parserLinks.setDateBaseService(dateBaseService);
    }

    public Site updatingSite(Site siteForIndex) {
        if (!(dateBaseService.findSiteByName(siteForIndex) == null)) {
            siteForIndex.setId(dateBaseService.findSiteByName(siteForIndex).getId());
        }
        Site siteInDateBase = dateBaseService.updateSite(siteForIndex, Status.INDEXING);
        siteInDateBase.setLastError("Нет ошибок");
        return siteInDateBase;
    }
}




