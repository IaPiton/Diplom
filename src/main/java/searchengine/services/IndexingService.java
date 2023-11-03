package searchengine.services;

import lombok.Data;
import lombok.Getter;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
    @Getter
    private static AtomicBoolean indexingRun = new AtomicBoolean(false);
    @Getter
    private static AtomicBoolean indexingStop = new AtomicBoolean(false);
    int countSite = 0;
    int finishedSite = 0;

    public ResultDto startIndexing() {
        if (indexingRun.get()) {
            new ResultDto(false, "Индексация уже запущена");
            log.info("Попытка повторного запуска индексации");
        } else {
            log.info("Индексация запущена");
            ArrayList<Site> sites = siteConfig.getSites();
            indexingRun.set(true);
            indexingStop.set(false);
            dateBaseService.deleteAllIndexes();
            dateBaseService.deleteAllPages();
            dateBaseService.deleteAllLemma();
            for (Site site : sites) {
                countSite++;
                CompletableFuture.runAsync(() -> {
                    try {
                        indexingSite(site);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }, ForkJoinPool.commonPool());
            }
        }

        return new ResultDto(true);
    }

    @Async
    public void indexingSite(Site siteForIndex) throws InterruptedException {
        Site siteInDateBase = updatingSite(siteForIndex);
        CopyOnWriteArraySet<String> linksSet = new CopyOnWriteArraySet<>();
        ParserLinksService parserLinks = new ParserLinksService(siteInDateBase.getUrl() + "/", linksSet, siteInDateBase);
        parseConfig(parserLinks);
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors());
        pool.invoke(parserLinks);
        indexingFinish(siteInDateBase);
        if (countSite == finishedSite) {
            indexingRun.set(false);
        }
    }

    public ResultDto startIndexingPage(String url) {
        if (indexingRun.get()) {
            return new ResultDto(false, "Индексация уже запущена, дождитесь " + "окончания индексации или остановите ее");
        } else {
            ArrayList<Site> sites = siteConfig.getSites();
            for (Site siteForIndex : sites) {
                if (url.toLowerCase().contains(siteForIndex.getUrl())) {
                    log.info("Страница - " + url + " - добавлена на переиндексацию");
                    indexingPage(url, siteForIndex);
                    return new ResultDto(true, "Страница - " + url + " - добавлена на переиндексацию");
                }
            }
            return new ResultDto(false, "Данная страница находится " + "за пределами сайтов, указаных в конфигурационном файле.");
        }
    }

    public ResultDto indexingPage(String url, Site siteForIndex) {
        indexingRun.set(true);
        indexingStop.set(false);
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
        indexingRun.set(false);
        return new ResultDto(true);
    }

    public ResultDto stopIndexing() {
        if (!indexingRun.get()) {
            new ResultDto(false, "Индексация не запущена").getError();
        } else {
            log.info("Остановка индексации");
            indexingRun.set(false);
            indexingStop.set(true);
        }
        return new ResultDto(true);
    }

    public void indexingFinish(Site siteInDateBase) {
        finishedSite++;
        if (indexingStop.get()) {
            siteInDateBase.setLastError("Индексация остановлена");
            dateBaseService.updateSite(siteInDateBase, Status.FAILED);
        } else {
            dateBaseService.updateSite(siteInDateBase, Status.INDEXED);
        }
        log.info("Индексация сайта " + siteInDateBase.getName() + " завершена");

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




