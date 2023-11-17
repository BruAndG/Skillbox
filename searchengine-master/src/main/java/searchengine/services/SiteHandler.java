package searchengine.services;

import searchengine.Application;
import searchengine.config.JsoupConnectConfig;
import searchengine.config.OtherSettings;
import searchengine.config.Site;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class SiteHandler implements Runnable, StartHandler {
    protected final Site site;
    protected final SiteRepository siteRepository;
    protected final PageRepository pageRepository;
    protected final LemmaRepository lemmaRepository;
    protected final IndexRepository indexRepository;
    protected final OtherSettings otherSettings;
    protected final JsoupConnectConfig jsoupConnectConfig;
    protected final IndexingService indexingService;
    protected final LemmaFinder lemmaFinder;

    protected volatile boolean error;

    public SiteHandler(Site site, SiteRepository siteRepository, PageRepository pageRepository
            , LemmaRepository lemmaRepository, IndexRepository indexRepository
            , OtherSettings otherSettings, JsoupConnectConfig jsoupConnectConfig, IndexingService indexingService
            , LemmaFinder lemmaFinder) {
        this.site = site;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.otherSettings = otherSettings;
        this.jsoupConnectConfig = jsoupConnectConfig;
        this.indexingService = indexingService;
        this.lemmaFinder = lemmaFinder;
    }

    protected SiteEntity createSiteEntity(Site site, StatusType statusType) {
        SiteEntity siteEntity = new SiteEntity();

        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatus(statusType);
        siteEntity.setStatusTime(LocalDateTime.now());

        return siteEntity;
    }

    protected SiteEntity getSiteEntity() {
        List<SiteEntity> siteEntityList = siteRepository.findByUrl(site.getUrl());
        for (SiteEntity siteEntity : siteEntityList) {
            siteRepository.delete(siteEntity);
        }

        return createSiteEntity(site, StatusType.INDEXING);
    }

    private void setStatus(SiteEntity siteEntity, StatusType status, String lastError) {
        siteEntity.setStatus(status);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteEntity.setLastError(lastError);
        siteRepository.save(siteEntity);
    }

    protected void deletePageWithContentIsNull(long siteId) {
        List<PageEntity> pageEntityList = pageRepository.findBySiteIdAndContentIsNull(siteId);
        if (!pageEntityList.isEmpty()) {
            pageRepository.deleteAllInBatch(pageEntityList);
        }
    }

    @Override
    public void run() {
        error = false;
        SiteEntity siteEntity = getSiteEntity();
        siteRepository.save(siteEntity);
        try {
            ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
            forkJoinPool.invoke(
                    new GetterSiteMap(siteEntity, siteEntity.getUrl(), this
                            , siteRepository, pageRepository, lemmaRepository, indexRepository
                            , otherSettings, jsoupConnectConfig
                            , indexingService, lemmaFinder
                    )
            );

            deletePageWithContentIsNull(siteEntity.getId());

            if (indexingService.isStopIndexingProcess()) {
                setStatus(siteEntity, StatusType.FAILED, otherSettings.getErrorMessageIndexingStoppedByUser());
            } else {
                setStatus(siteEntity, StatusType.INDEXED, null);
            }
        } catch (Exception e) {
            Application.log.info(e.getLocalizedMessage());
            setStatus(siteEntity, StatusType.FAILED, e.getLocalizedMessage());
        }
    }

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
}
