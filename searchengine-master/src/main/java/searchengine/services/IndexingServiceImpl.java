package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import searchengine.Application;
import searchengine.config.JsoupConnectConfig;
import searchengine.config.OtherSettings;
import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseServ;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {
    private final SitesList sites;
    private final OtherSettings otherSettings;
    private final JsoupConnectConfig jsoupConnectConfig;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final LuceneMorphology luceneMorphology;
    private LemmaFinder lemmaFinder = null;

    private volatile boolean stopIndexingProcess;

    private LemmaFinder getLemmaFinder() {
        if (lemmaFinder == null) {
            lemmaFinder = new LemmaFinder(luceneMorphology);
        }
        return lemmaFinder;
    }

    private IndexingResponseServ createResponse(HttpStatus httpStatus, String errorMessage) {
        IndexingResponseServ responseServ = new IndexingResponseServ();
        IndexingResponse response = new IndexingResponse();
        responseServ.setIndexingResponse(response);
        responseServ.setStatus(httpStatus);
        response.setResult(httpStatus.equals(HttpStatus.OK));
        response.setError(errorMessage);

        return responseServ;
    }

    private List<Site> getListForIndexing() {

        List<Site> result = new ArrayList<>();

        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            List<SiteEntity> siteEntityList = siteRepository.findByUrl(site.getUrl()); //getSiteByUrl(site);
            boolean suitableForIndexing = siteEntityList.isEmpty();
            for (SiteEntity siteEntity : siteEntityList) {
                if (!siteEntity.getStatus().equals(StatusType.INDEXING)) {
                    suitableForIndexing = true;
                    break;
                }
            }
            if (suitableForIndexing) {
                result.add(site);
            }
        }

        return result;
    }

    private Site findSite(String url) {
        Site rootSite = null;

        // поиск сайта
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            String regexUrlFilter = Utils.getRegexToFilterUrl(site.getUrl());
            if (Utils.isCorrectDomain(url, regexUrlFilter) && !url.contains("#") && !Utils.isFile(url)) {
                rootSite = site;
                break;
            }
        }

        return rootSite;
    }

    @Override
    public IndexingResponseServ startIndexing() {
        IndexingResponseServ responseServ = null;
        stopIndexingProcess = false;
        try {
            List<Site> sitesList = getListForIndexing();
            if (sitesList.size() > 0) {
                for (Site site : sitesList) {
                    ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
                    forkJoinPool.execute(
                            new SiteHandler(site, siteRepository, pageRepository, lemmaRepository, indexRepository
                                    , otherSettings, jsoupConnectConfig
                                    , this, getLemmaFinder()
                            )
                    );
                }

                responseServ = createResponse(HttpStatus.OK, null);
            } else {
                responseServ = createResponse(HttpStatus.NOT_FOUND, otherSettings.getErrorMessageIsAlreadyStarted());
            }
        } catch (Exception e) {
            Application.log.info(e.getLocalizedMessage());
            responseServ = createResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }

        return responseServ;
    }

    @Override
    public IndexingResponseServ stopIndexing() {
        IndexingResponseServ responseServ = null;
        try {
            if (siteRepository.existsByStatus(StatusType.INDEXING)) {
                stopIndexingProcess = true;
                responseServ = createResponse(HttpStatus.OK, null);
            } else {
                responseServ = createResponse(HttpStatus.NOT_FOUND, otherSettings.getErrorMessageIsNotRunning());
            }
        } catch (Exception e) {
            Application.log.info(e.getLocalizedMessage());
            responseServ = createResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }

        return responseServ;
    }

    @Override
    public IndexingResponseServ indexingPage(String url) {
        IndexingResponseServ responseServ = null;
        stopIndexingProcess = false;
        try {
            Site site = findSite(url);
            if (site != null) {
                ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
                forkJoinPool.execute(
                        new PageHandler(site, url, siteRepository, pageRepository, lemmaRepository, indexRepository
                                , otherSettings, jsoupConnectConfig
                                , this, getLemmaFinder()
                        )
                );

                responseServ = createResponse(HttpStatus.OK, null);
            } else {
                responseServ = createResponse(HttpStatus.BAD_REQUEST, otherSettings.getErrorPageLocatedOutsideSites());
            }
        } catch (Exception e) {
            Application.log.info(e.getLocalizedMessage());
            responseServ = createResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }

        return responseServ;
    }

    @Override
    public boolean isStopIndexingProcess() {
        return stopIndexingProcess;
    }
}
