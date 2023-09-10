package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.config.SiteConfig;
import searchengine.dto.*;

import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repository.PageRepository;
import searchengine.repository.SiteRepository;

import java.util.ArrayList;

import java.util.List;
import java.util.Random;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    private final Random random = new Random();
    private final SiteConfig siteConfig;

    @Override
    public StatisticsResponse getStatistics() {
            TotalStatistics total = new TotalStatistics(siteRepository.count(), pageRepository.count(), true);

            List<DetailedStatisticsItem> detailedList = new ArrayList<>();
            siteRepository.findAll().forEach(site -> {
                DetailedStatisticsItem detailed = new DetailedStatisticsItem(site.getUrl(), site.getName(), site.getStatus(),
                        site.getStatusTime(), site.getLastError(), pageRepository.countBySiteByPage(site));
                detailedList.add(detailed);
            });
            StatisticsData statisticsDate = new StatisticsData(total, detailedList) {

            };

            return new StatisticsResponse(true, statisticsDate);
        }
    }
//        String[] statuses = { "INDEXED", "FAILED", "INDEXING" };
//        String[] errors = {
//                "Ошибка индексации: главная страница сайта не доступна",
//                "Ошибка индексации: сайт не доступен",
//                ""
//        };
//
//        TotalStatistics total = new TotalStatistics();
//        total.setSites(siteConfig.getSites().size());
//
//        total.setIndexing(true);
//
//        List<DetailedStatisticsItem> detailed = new ArrayList<>();
//        List<Site> sitesList = siteConfig.getSites();
//
//
//        for(int i = 0; i < sitesList.size(); i++) {
//            Site site = sitesList.get(i);
//            DetailedStatisticsItem item = new DetailedStatisticsItem();
//            item.setName(site.getName());
//            item.setUrl(site.getUrl());
//            int pages = random.nextInt(1_000);
//            int lemmas = pages * random.nextInt(1_000);
//            item.setPages(pages);
//            item.setLemmas(lemmas);
//            item.setStatus(statuses[i % 3]);
//            item.setError(errors[i % 3]);
//            item.setStatusTime(System.currentTimeMillis() -
//                    (random.nextInt(10_000)));
//            total.setPages(total.getPages() + pages);
//            total.setLemmas(total.getLemmas() + lemmas);
//            detailed.add(item);
//        }
//
//        StatisticsResponse response = new StatisticsResponse();
//        StatisticsData data = new StatisticsData();
//        data.setTotal(total);
//        data.setDetailed(detailed);
//        response.setStatistics(data);
//        response.setResult(true);
//        return response;
//    }

