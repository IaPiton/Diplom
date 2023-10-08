package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SiteConfig;
import searchengine.dto.ResponseError;

import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.services.DateBaseService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final DateBaseService dateBaseService;
    private final SiteConfig siteConfig;


    private SiteConfig sites;
    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, DateBaseService dateBaseService, SiteConfig siteConfig) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.dateBaseService = dateBaseService;
        this.siteConfig = siteConfig;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (!dateBaseService.getIndexingRun().get()) {
            log.info("Индексация запущена");
            return ResponseEntity.ok(indexingService.startIndexing());
        }
        return ResponseEntity.badRequest().body(new ResponseError("Индексация уже запущена"));
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        log.info("Остановка индексации");
        if (!dateBaseService.getIndexingRun().get()) {
            return ResponseEntity.badRequest().body(new ResponseError("Индексация не запущена"));
        }
        return ResponseEntity.ok(indexingService.stopIndexing());
    }
    @PostMapping("/indexPage")
    public ResponseEntity<Object> getPage(@RequestParam(name = "url") String url)
            throws SQLException, IOException,ParserConfigurationException {
        if (!dateBaseService.getIndexingRun().get()) {
            ArrayList<Site> sites = siteConfig.getSites();
            for (Site site : sites) {
                if (url.toLowerCase().contains(site.getUrl())) {
                    log.info("Страница - " + url + " - добавлена на переиндексацию");
                    return ResponseEntity.ok(indexingService.indexingPage(url, site));
                }
            }
            log.info("Указанная страница" + "за пределами конфигурационного файла");
            return ResponseEntity.badRequest().body(new ResponseError("Данная страница находится " +
                    "за пределами сайтов,указаных в конфигурационном файле."));
        }
        return ResponseEntity.badRequest().body(new ResponseError("Индексация уже запущена. " +
                "Остановите индексацию, или дождитесь ее окончания"));
    }

}


