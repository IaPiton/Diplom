package searchengine.controllers;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import searchengine.dto.ResultDto;
import searchengine.dto.SearchDto;
import searchengine.dto.statistics.StatisticsResponse;

import searchengine.model.Site;
import searchengine.model.Status;
import searchengine.repository.SiteRepository;
import searchengine.services.IndexingService;
import searchengine.services.SearchService;
import searchengine.services.StatisticsService;
import searchengine.utils.DateBaseService;

import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
@Data
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SiteRepository siteRepository;
    private final SearchService searchService;

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
            @RequestParam(value = "limit", defaultValue = "5", required = false) int limit) {
        List<SearchDto> searchData;
        if (query.isEmpty()) {
            return new ResultDto(false, "Задан пустой поисковый запрос");
        }
        if (!(site == null)) {
            if (siteRepository.findByUrl(site) == null) {
                return new ResultDto(false, "Данная страница находится за пределами сайтов,\n" +
                        "указанных в конфигурационном файле", HttpStatus.BAD_REQUEST);
            } else {
                List<Site> siteList = siteRepository.findByUrl(site);
                if ((siteList.get(0).getStatus() != Status.INDEXED)) {
                    return new ResultDto(false, "Сайт индексируется");
                }
                searchData = searchService.search(query, siteList, offset, 30);
            }
        } else {
            if (DateBaseService.getIndexingRun().get()) {
                return new ResultDto(false, "Идет индексация сайтов", HttpStatus.BAD_REQUEST);
            }
            searchData = searchService.fullSearch(query, offset, 30);
        }
        return new ResultDto(true, DateBaseService.getCountPage().get(), searchData, HttpStatus.OK);
    }
}


