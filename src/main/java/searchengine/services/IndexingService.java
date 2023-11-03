package searchengine.services;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


import searchengine.config.ParserConfig;
import searchengine.config.SiteConfig;

import searchengine.dto.ResultDto;
import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

@Service
@Data
@Slf4j
public class IndexingService {
    @Autowired
    private ParserConfig parserConfig;
    @Autowired
    private final DateBaseService dateBaseService;
    @Autowired
    private SiteConfig siteConfig;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexesRepository indexesRepository;
    @Autowired
    private PageRepository pageRepository;

    public ResultDto startIndexing() {
     if (dateBaseService.getIndexingRun().get()) {
         new ResultDto(false, "Индексация уже запущена").getError();
     } else {
         log.info("Индексация запущена");
         ArrayList<Site> sites = siteConfig.getSites();
         dateBaseService.getIndexingRun().compareAndSet(false, true);
         dateBaseService.getIndexingStop().compareAndSet(true, false);
         dateBaseService.deleteAllIndexes();
         dateBaseService.deleteAllPages();
         dateBaseService.deleteAllLemma();
         for (Site site : sites) {
             CompletableFuture.runAsync(() -> {
                 indexingSite(site);
             }, ForkJoinPool.commonPool());
         }
     }
        return new ResultDto(true);
    }

    @Async
    public void indexingSite(Site siteForIndex) {
        Site siteInDateBase = updatingSite(siteForIndex);
        CopyOnWriteArraySet<String> linksSet = new CopyOnWriteArraySet<>();
        ParserLinksService parserLinks = new ParserLinksService(siteInDateBase.getUrl() + "/", linksSet, siteInDateBase);
        parseConfig(parserLinks);
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.invoke(parserLinks);
        indexingFinish(siteInDateBase);
    }
    public ResultDto startIndexingPage (String url){
        String result = "";
        Boolean resultResponse = true;
        if (dateBaseService.getIndexingRun().get()) {
            resultResponse = false;
           result = "Индексация уже запущена. " +
                    "Остановите индексацию, или дождитесь ее окончания";
        }else {
            ArrayList<Site> sites = siteConfig.getSites();
            for (Site siteForIndex : sites) {
                if (url.toLowerCase().contains(siteForIndex.getUrl())) {

                    log.info("Страница - " + url + " - добавлена на переиндексацию");
                    indexingPage(url, siteForIndex);
                    return new ResultDto(true);
                }

            }
            return new ResultDto(false, "Данная страница находится " +
                    "за пределами сайтов, указаных в конфигурационном файле.");
        }
        return new ResultDto(resultResponse, result);
    }

    public ResultDto indexingPage(String url, Site siteForIndex) {
        dateBaseService.getIndexingRun().compareAndSet(false,true);
        dateBaseService.getIndexingStop().compareAndSet(true, false);
        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        if (!(dateBaseService.findPathByPage(path) == null)) {
            List<Integer> idPath = dateBaseService.findPathByPage(path);
            for (Integer idPage : idPath) {
                List<Integer> lemmaId = dateBaseService.lemmaIdByPath(idPage);
                for (Integer id : lemmaId) {
                    indexesRepository.deleteIndexPathByPage(id);
                    if (lemmaRepository.frequencyById(id) == 1) {
                        lemmaRepository.deleteLemmaPathByPage(id);
                    } else {
                        lemmaRepository.updateLemmaFrequencyDelete(id);
                    }

                }

                pageRepository.deletePathByPage(idPage);
            }
        }
        Site siteInDateBase = updatingSite(siteForIndex);
        CopyOnWriteArraySet<String> linksSet = new CopyOnWriteArraySet<>();
        ParserLinksService parserLinks = new ParserLinksService(url, linksSet, siteInDateBase);
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        parseConfig(parserLinks);
        pool.invoke(parserLinks);
        indexingFinish(siteInDateBase);
    return new ResultDto(true);
}
    public ResultDto stopIndexing() {
        if (!dateBaseService.getIndexingRun().get()) {
            new ResultDto(false, "Индексация не запущена").getError();
        } else {
            log.info("Остановка индексации");
            dateBaseService.getIndexingRun().compareAndSet(true, false);
            dateBaseService.getIndexingStop().compareAndSet(false, true);
        }
        return new ResultDto(true);
    }

    public void indexingFinish(Site siteInDateBase) {
        if (dateBaseService.getIndexingStop().get()) {
            siteInDateBase.setLastError("Индексация остановлена");
            dateBaseService.updateSite(siteInDateBase, Status.FAILED);
        } else {
            dateBaseService.updateSite(siteInDateBase, Status.INDEXED);
        }
log.info("Индексация завершена");
        dateBaseService.getIndexingRun().compareAndSet(true,false);
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




