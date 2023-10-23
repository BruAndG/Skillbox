package searchengine.services;

import searchengine.config.JsoupConnectConfig;
import searchengine.config.OtherSettings;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.concurrent.ForkJoinPool;

public class SiteHandler implements Runnable, StartHandler {
    private final SiteEntity rootSiteEntity;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JsoupConnectConfig jsoupConnectConfig;
    private final OtherSettings otherSettings;
    private final IndexingService indexingService;
    private final LemmaFinder lemmaFinder;
    private final Utils utils;

    private volatile boolean error;

    @Override
    public boolean isError() {
        return error;
    }

    @Override
    public void setError(boolean error) {
        this.error = error;
    }

    @Override
    public boolean isOunPage() {
        return false;
    }

    public SiteHandler(SiteEntity rootSiteEntity
            , SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository
            , IndexRepository indexRepository
            , JsoupConnectConfig jsoupConnectConfig, OtherSettings otherSettings, IndexingService indexingService
            , LemmaFinder lemmaFinder, Utils utils) {
        this.rootSiteEntity = rootSiteEntity;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.jsoupConnectConfig = jsoupConnectConfig;
        this.otherSettings = otherSettings;
        this.indexingService = indexingService;
        this.lemmaFinder = lemmaFinder;
        this.utils = utils;
    }

    @Transactional
    private void setStatus(SiteEntity siteEntity, StatusType status, String lastError) {
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError(lastError);
        siteRepository.save(siteEntity);
    }

    @Override
    public void run() {

        error = false;

        try {
            ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
            forkJoinPool.invoke(
                    new GetterSiteMap(
                            rootSiteEntity, rootSiteEntity.getUrl()
                            , siteRepository, pageRepository, lemmaRepository, indexRepository
                            , jsoupConnectConfig, otherSettings
                            , indexingService, this, lemmaFinder, utils

                    )
            );
        } catch (Exception e) {
            setStatus(rootSiteEntity, StatusType.FAILED, e.getMessage());
            return;
        }

        if (indexingService.isStopIndexingProcess()) {
            setStatus(rootSiteEntity, StatusType.FAILED, otherSettings.getErrorMessageIndexingStoppedByUser());
        } else {
            setStatus(rootSiteEntity, StatusType.INDEXED, null);
        }

    }
}
