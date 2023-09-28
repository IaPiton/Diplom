package searchengine.services;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import searchengine.config.ParserConfig;
import searchengine.config.SiteConfig;
import searchengine.dto.IndexingRunAndStop;
import searchengine.dto.ResponseTrue;
import searchengine.model.Site;
import searchengine.model.Status;
import utils.ParserLinks;


import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicBoolean;

@Data
@Service
public class IndexingService {
    @Autowired
    private ParserConfig parserConfig;
    @Autowired
    private final DateBaseService dateBaseService;
    @Autowired
    private SiteConfig siteConfig;
    public IndexingRunAndStop indexingRunAndStop = new IndexingRunAndStop();


    public ResponseTrue startIndexing()
    {
        ArrayList<Site> sites = siteConfig.getSites();
        indexingRunAndStop.getIndexingRun().set(true);
        indexingRunAndStop.getIndexingStop().set(false);
        dateBaseService.deleteAllPages();
        for (Site site : sites) {
            CompletableFuture.runAsync(() -> {
                indexingSite(site);

            }, ForkJoinPool.commonPool());
        }
        return new ResponseTrue("true");
    }

    @Async
    public void indexingSite(Site siteForIndex) {
        if (!(dateBaseService.findSiteByName(siteForIndex) == null))
        {
            siteForIndex.setId(dateBaseService.findSiteByName(siteForIndex).getId());
        }
        Site siteInDateBase = dateBaseService.updateSite(siteForIndex, Status.INDEXING);
        CopyOnWriteArraySet<String> linksSet = new CopyOnWriteArraySet<>();
        ParserLinks parserLinks = new ParserLinks(siteInDateBase.getUrl() + "/", linksSet, siteInDateBase);
        parserLinks.setParserConfig(parserConfig);
        parserLinks.setDateBaseService(dateBaseService);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parserLinks);
        if (indexingRunAndStop.getIndexingStop().get()) {
                siteInDateBase.setLastError("Индексация остановлена");
               dateBaseService.updateSite(siteInDateBase, Status.FAILED);
           } else {
                dateBaseService.updateSite(siteInDateBase, Status.INDEXED);
           }
        indexingRunAndStop.getIndexingRun().set(false);
        }
    public Object stopIndexing() {
        indexingRunAndStop.getIndexingRun().set(false);
        indexingRunAndStop.getIndexingStop().set(true);
        return new ResponseTrue("true");
    }
    }




