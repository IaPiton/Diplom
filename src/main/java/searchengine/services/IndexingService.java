package searchengine.services;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import searchengine.config.ParserConfig;
import searchengine.config.SiteConfig;
import searchengine.dto.ResponseTrue;
import searchengine.model.Site;
import searchengine.model.Status;
import utils.ParserLinks;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
@Data
@Service
public class IndexingService {
    @Autowired
    private ParserConfig parserConfig;
    @Autowired
    private final DateBaseService dateBaseService;
    @Autowired
    private SiteConfig siteConfig;
    public ResponseTrue startIndexing()
    {
        ArrayList<Site> sites = siteConfig.getSites();
        dateBaseService.setIndexingRun(true);
        dateBaseService.setIndexingStop(false);
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
        Set<String> linksSet = Collections.synchronizedSet(new HashSet<>());
        ParserLinks parserLinks = new ParserLinks(siteInDateBase.getUrl() + "/", siteInDateBase, linksSet);
        parserLinks.setParserConfig(parserConfig);
        parserLinks.setDateBaseService(dateBaseService);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parserLinks);
        if (dateBaseService.isIndexingStop()) {
                siteInDateBase.setLastError("Индексация остановлена");
               dateBaseService.updateSite(siteInDateBase, Status.FAILED);
           } else {
                dateBaseService.updateSite(siteInDateBase, Status.INDEXED);
           }
            dateBaseService.setIndexingRun(false);
        }
    public Object stopIndexing() {
        dateBaseService.setIndexingRun(false);
        dateBaseService.setIndexingStop(true);
        return new ResponseTrue("true");
    }
    }




