package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.dto.statistics.*;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;

    private StatisticsResponse formingStatistics() {
        TotalStatistics total = new TotalStatistics();
        List<DetailedStatisticsItem> detailed = new ArrayList<>();

        List<SiteEntity> siteEntityList = siteRepository.findAll();
        total.setSites(siteEntityList.size());

        boolean indexing = false;
        for (SiteEntity siteEntity : siteEntityList) {
            if (siteEntity.getStatus() == StatusType.INDEXING) {
                indexing = true;
            }

            DetailedStatisticsItem item = new DetailedStatisticsItem();
            item.setName(siteEntity.getName());
            item.setUrl(siteEntity.getUrl());

            int pages = pageRepository.countBySiteId(siteEntity.getId());
            int lemmas = lemmaRepository.countBySiteId(siteEntity.getId());

            item.setPages(pages);
            item.setLemmas(lemmas);

            item.setStatus(siteEntity.getStatus().toString());
            item.setError(siteEntity.getLastError());

            ZonedDateTime zdt = ZonedDateTime.of(siteEntity.getStatusTime(), ZoneId.systemDefault());
            item.setStatusTime(zdt.toInstant().toEpochMilli());

            total.setPages(total.getPages() + pages);
            total.setLemmas(total.getLemmas() + lemmas);

            detailed.add(item);
        }

        total.setIndexing(indexing);

        StatisticsResponse response = new StatisticsResponse();
        StatisticsData data = new StatisticsData();
        data.setTotal(total);
        data.setDetailed(detailed);
        response.setStatistics(data);
        response.setResult(true);

        return response;
    }

    private StatisticsResponse formingStatisticsError(String error) {
        StatisticsResponse response = new StatisticsResponse();

        response.setResult(false);
        response.setStatistics(null);

        return response;
    }

    @Override
    public StatisticsResponseServ getStatistics() {
        StatisticsResponseServ responseServ = new StatisticsResponseServ();
        try {
            StatisticsResponse response = formingStatistics();
            responseServ.setStatisticsResponse(response);
            responseServ.setStatus(HttpStatus.OK);
        } catch (Exception e) {
            Application.log.info(e.getLocalizedMessage());
            StatisticsResponse response = formingStatisticsError(e.getLocalizedMessage());
            responseServ.setStatisticsResponse(response);
            responseServ.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return responseServ;
    }
}
