package searchengine.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import searchengine.config.SiteConfig;
import searchengine.dto.ResponseError;

import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.DateBaseService;
import searchengine.services.IndexingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final DateBaseService dateBaseService;

    private SiteConfig sites;
    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, DateBaseService dateBaseService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.dateBaseService = dateBaseService;
    }
    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


    @GetMapping("/startIndexing")
    public ResponseEntity<Object> startIndexing() {
        if (!dateBaseService.isIndexingRun()) {
            return ResponseEntity.ok(indexingService.startIndexing());
        }
        return ResponseEntity.badRequest().body(new ResponseError("Индексация уже запущена"));
    }
    @GetMapping("/stopIndexing")
    public ResponseEntity<Object> stopIndexing() {
        if (!dateBaseService.isIndexingRun()) {
            return ResponseEntity.badRequest().body(new ResponseError("Индексация не запущена"));
        }
        return ResponseEntity.ok(indexingService.stopIndexing());
    }
    }


