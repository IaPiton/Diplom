package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.config.SiteConfig;


import searchengine.dto.statistics.DetailedStatisticsItem;
import searchengine.dto.statistics.StatisticsData;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.TotalStatistics;
import searchengine.repository.LemmaRepository;
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
    @Autowired
    private LemmaRepository lemmaRepository;

    private final SiteConfig siteConfig;

    @Override
    public StatisticsResponse getStatistics() {
        TotalStatistics total = new TotalStatistics(siteRepository.count(), pageRepository.count(),
                lemmaRepository.count(), true);

        List<DetailedStatisticsItem> detailedList = new ArrayList<>();
        siteRepository.findAll().forEach(site -> {
            DetailedStatisticsItem detailed = new DetailedStatisticsItem(site.getUrl(), site.getName(), site.getStatus(),
                    site.getStatusTime(), site.getLastError(), pageRepository.countBySiteByPage(site), lemmaRepository.countBySiteByLemma(site));
            detailedList.add(detailed);
        });
        StatisticsData statisticsDate = new StatisticsData(total, detailedList) {

        };

        return new StatisticsResponse(true, statisticsDate);
    }
}


