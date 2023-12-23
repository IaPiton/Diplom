package searchengine.services;

import lombok.Data;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import searchengine.config.ParserConfig;
import searchengine.config.SiteConfig;

import searchengine.dto.ResultDto;
import searchengine.model.*;
import searchengine.repository.IndexesRepository;
import searchengine.repository.LemmaRepository;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;
import searchengine.utils.DateBaseService;
import searchengine.utils.ParserLinks;


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

    private final ParserConfig parserConfig;

    private final DateBaseService dateBaseService;

    private final SiteConfig siteConfig;

    private final LemmaRepository lemmaRepository;

    private final IndexesRepository indexesRepository;

    private final PageRepository pageRepository;
    private final SiteRepository siteRepository;

    public ResultDto startIndexing() {
        if (DateBaseService.getIndexingRun().get()) {
            new ResultDto(false, "Индексация уже запущена");
            log.info("Попытка повторного запуска индексации");
        } else {
            log.info("Индексация запущена");
            ArrayList<Site> sites = siteConfig.getSites();
            DateBaseService.setIndexingRun(new AtomicBoolean(true));
            DateBaseService.setIndexingStop(new AtomicBoolean(false));
            dateBaseService.deleteAll();
            for (Site site : sites) {
                DateBaseService.getCountSite().incrementAndGet();
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
        ParserLinks parserLinks = new ParserLinks(siteInDateBase.getUrl() + "/", linksSet, siteInDateBase);
        parseConfig(parserLinks);
        ForkJoinPool pool = new ForkJoinPool();
        pool.invoke(parserLinks);
        indexingFinish(siteInDateBase);
        if (DateBaseService.getCountSite().get() == DateBaseService.getIndexedSite().get()) {
            DateBaseService.setIndexingRun(new AtomicBoolean(false));
        }
    }

    @SneakyThrows
    public ResultDto startIndexingPage(String url) {
        if (DateBaseService.getIndexingRun().get()) {
            return new ResultDto(false, "Индексация уже запущена, дождитесь " + "окончания индексации или остановите ее");
        } else {
            ArrayList<Site> sites = siteConfig.getSites();
            Site site = conteinsUrl(sites, url);
                if (site.getUrl() != null) {
                log.info("Страница - " + url + " - добавлена на переиндексацию");
                indexingPage(url, site);
                return new ResultDto(true);
            }
        }
        log.info("Страница - " + url + " находится " +
                "за пределами сайтов, указаных в конфигурационном файле.");
        return new ResultDto(false, "Данная страница находится "
                + "за пределами сайтов, указаных в конфигурационном файле.");
    }

    private Site conteinsUrl(ArrayList<Site> sites, String url) {
        for (Site site : sites) {
            if (url.toLowerCase().contains(site.getUrl())) {
                return site;
            }
        }
        return new Site();
    }

    public void indexingPage(String url, Site siteForIndex) {
        DateBaseService.setIndexingRun(new AtomicBoolean(true));
        DateBaseService.setIndexingStop(new AtomicBoolean(false));
        deletePage(url, siteForIndex);
    }

    public void deletePage(String url, Site siteForIndex) {
        String path = url.substring(url.indexOf('/', url.indexOf(".")));
        if (pageRepository.existsByPath(path)) {
            Page page = pageRepository.findPageByPath(path);
            List<Indexes> indexesList = indexesRepository.findIndexesByPageByIndex(page);
            dateBaseService.lemmaByIndexesUpdate(indexesList);
            pageRepository.delete(page);
        }

        indexesPage(url, siteForIndex);
    }

    public void indexesPage(String url, Site siteForIndex) {
        Site siteInDateBase = updatingSite(siteForIndex);
        ParserLinks parserLinks = new ParserLinks();
        parseConfig(parserLinks);
        parserLinks.parserPage(url, siteForIndex);
        indexingFinish(siteInDateBase);
        DateBaseService.setIndexingRun(new AtomicBoolean(false));
    }

    public ResultDto stopIndexing() {
        if (!DateBaseService.getIndexingRun().get()) {
            new ResultDto(false, "Индексация не запущена");
        } else {
            log.info("Остановка индексации");
            DateBaseService.setIndexingRun(new AtomicBoolean(false));
            DateBaseService.setIndexingStop(new AtomicBoolean(true));
        }
        return new ResultDto(true);
    }

    public void indexingFinish(Site siteInDateBase) {
        DateBaseService.getIndexedSite().incrementAndGet();
        if (DateBaseService.getIndexingStop().get()) {
            siteInDateBase.setLastError("Индексация остановлена");
            dateBaseService.updateSite(siteInDateBase, Status.FAILED);
        } else {
            dateBaseService.updateSite(siteInDateBase, Status.INDEXED);
        }
        log.info("Индексация сайта " + siteInDateBase.getName() + " завершена");
    }

    public void parseConfig(ParserLinks parserLinks) {
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




