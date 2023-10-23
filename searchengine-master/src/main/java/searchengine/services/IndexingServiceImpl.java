package searchengine.services;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.lucene.morphology.LuceneMorphology;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
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

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndexingServiceImpl implements IndexingService {

    private final SitesList sites;
    private final JsoupConnectConfig jsoupConnectConfig;
    private final OtherSettings otherSettings;

    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;

    private final LuceneMorphology luceneMorphology;
    private LemmaFinder lemmaFinder = null;

    private final Utils utils;

    @Getter
    private volatile boolean stopIndexingProcess;

    private LemmaFinder getLemmaFinder() {
        if (lemmaFinder == null) {
            lemmaFinder = new LemmaFinder(luceneMorphology);
        }
        return lemmaFinder;
    }

    private List<SiteEntity> getSiteByStatus(StatusType status) {
        return siteRepository.findByStatus(status);
    }

    private boolean isEqualsStatus(List<SiteEntity> siteEntityList, StatusType status) {
        for (SiteEntity siteEntity : siteEntityList) {
            if (siteEntity.getStatus() == status) {
                return true;
            }
        }
        return false;
    }

    private void deleteListSiteEntity(List<SiteEntity> siteEntityList) {
        for (SiteEntity siteEntity : siteEntityList) {
            siteRepository.delete(siteEntity);
        }
    }

    @Transactional
    private List<SiteEntity> getListForIndexing() {

        List<SiteEntity> result = new ArrayList<>();

        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            boolean isNewRow = false;
            List<SiteEntity> siteEntityList = siteRepository.findByUrl(site.getUrl()); //getSiteByUrl(site);
            if ((siteEntityList == null) || (siteEntityList.size() == 0)) {
                isNewRow = true;
            } else if (!isEqualsStatus(siteEntityList, StatusType.INDEXING)) {
                deleteListSiteEntity(siteEntityList);
                isNewRow = true;
            }
            if (isNewRow) {
                SiteEntity newSite = utils.createSiteEntity(site, StatusType.INDEXING); // createSiteEntity(site);
                result.add(newSite);
                siteRepository.save(newSite);
            }
        }

        return result;
    }

    @Override
    public IndexingResponseServ startIndexing() {
        IndexingResponseServ responseServ = new IndexingResponseServ();
        IndexingResponse response;
        try {
            stopIndexingProcess = false;

            List<SiteEntity> listForIndexing;
            synchronized (this) {
                listForIndexing = getListForIndexing();
            }

            boolean isRun = listForIndexing.size() > 0;

            if (isRun) {
                for (SiteEntity siteEntity : listForIndexing) {
                    new Thread(
                            new SiteHandler(siteEntity, siteRepository, pageRepository, lemmaRepository, indexRepository
                                    , jsoupConnectConfig, otherSettings, this, getLemmaFinder(), utils)
                    ).start();
                }

                response = getResponse(true, null);
                responseServ.setStatus(HttpStatus.OK);
            } else {
                response = getResponse(false, otherSettings.getErrorMessageIsAlreadyStarted());
                responseServ.setStatus(HttpStatus.NOT_FOUND);
            }

        } catch (Exception e) {
            response = getResponse(false, e.getLocalizedMessage());
            responseServ.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseServ.setIndexingResponse(response);

        return responseServ;
    }

    @Override
    public IndexingResponseServ stopIndexing() {
        IndexingResponseServ responseServ = new IndexingResponseServ();
        IndexingResponse response;
        try {
            List<SiteEntity> listIndexing = getSiteByStatus(StatusType.INDEXING);
            boolean isRun = (listIndexing != null) && (listIndexing.size() > 0);
            stopIndexingProcess = true;
            if (isRun) {
                response = getResponse(true, null);
                responseServ.setStatus(HttpStatus.OK);
            } else {
                response = getResponse(false, otherSettings.getErrorMessageIsNotRunning());
                responseServ.setStatus(HttpStatus.NOT_FOUND);
            }
        } catch (Exception e) {
            response = getResponse(false, e.getLocalizedMessage());
            responseServ.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseServ.setIndexingResponse(response);

        return responseServ;
    }

    /*
    Поиск сайта в конфиг. файле
     */
    private Site findSite(String url) {
        //Utils utils = new Utils();
        Site rootSite = null;

        // поиск сайта
        List<Site> sitesList = sites.getSites();
        for (Site site : sitesList) {
            String regexUrlFilter = utils.getRegexToFilterUrl(site.getUrl());
            if (utils.isCorrectDomain(url, regexUrlFilter)) {
                rootSite = site;
                break;
            }
        }

        return rootSite;
    }

    private IndexingResponse getResponse(boolean result, String error) {
        IndexingResponse response = new IndexingResponse();
        response.setResult(result);
        response.setError(error);
        return response;
    }

    @Override
    public IndexingResponseServ indexingPage(String url) {
        IndexingResponseServ responseServ = new IndexingResponseServ();
        IndexingResponse response;
        try {
            stopIndexingProcess = false;

            Site rootSite = findSite(url);
            if (rootSite != null) {
                new Thread(
                        new PageHandler(rootSite, url, siteRepository, pageRepository, lemmaRepository, indexRepository
                                , jsoupConnectConfig, otherSettings, this, utils, getLemmaFinder())
                ).start();

                response = getResponse(true, null);
                responseServ.setStatus(HttpStatus.OK);
            } else {
                response = getResponse(false, otherSettings.getErrorPageLocatedOutsideSites());
                responseServ.setStatus(HttpStatus.BAD_REQUEST);
            }

        } catch (Exception e) {
            response = getResponse(false, e.getLocalizedMessage());
            responseServ.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        responseServ.setIndexingResponse(response);

        return responseServ;
    }

}
