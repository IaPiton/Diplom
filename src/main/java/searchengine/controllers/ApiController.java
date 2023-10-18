package searchengine.controllers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.config.SiteConfig;
import searchengine.dto.ResponseError;

import searchengine.dto.ResultDto;
import searchengine.dto.SearchDto;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.model.Site;
import searchengine.repository.SiteRepository;
import searchengine.services.DateBaseService;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class ApiController {

    private final StatisticsService statisticsService;

    private final IndexingService indexingService;

    private final DateBaseService dateBaseService;
    private final SiteRepository siteRepository;
    private final SiteConfig siteConfig;
    private final SearchService searchService;


    private SiteConfig sites;

    @Autowired
    public ApiController(StatisticsService statisticsService, IndexingService indexingService, DateBaseService dateBaseService, SiteRepository siteRepository, SiteConfig siteConfig, SearchService searchService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.dateBaseService = dateBaseService;
        this.siteRepository = siteRepository;
        this.siteConfig = siteConfig;
        this.searchService = searchService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }


    @GetMapping("/startIndexing")
    public ResultDto startIndexing() {
        return indexingService.startIndexing();
    }

    @GetMapping("/stopIndexing")
    public ResultDto stopIndexing() {
        return indexingService.stopIndexing();
    }

    @PostMapping("/indexPage")
    public ResultDto getPage(@RequestParam(name = "url") String url) {
        if (url.isEmpty()) {
            log.info("Страница не указана");
            return new ResultDto(false, "Страница не указана", HttpStatus.BAD_REQUEST);
        }
        return indexingService.startIndexingPage(url);
    }

    @GetMapping("/search")
    public ResultDto search(
            @RequestParam(value = "query") String query,
            @RequestParam(value = "site", required = false) String site,
            @RequestParam(value = "offset", defaultValue = "0", required = false) int offset,
            @RequestParam(value = "limit", defaultValue = "20", required = false) int limit) {
        List<SearchDto> searchData;
        if (!(site == null)) {
            if (siteRepository.findByUrl(site) == null) {
                return new ResultDto(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST);
            } else {
                searchData = searchService.search(query, site, offset, 30);
            }
        } else {
            searchData = searchService.fullSearch(query, offset, 30);
        }
        return new ResultDto(true, searchData.size(), searchData, HttpStatus.OK);
    }


}


