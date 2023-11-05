package searchengine.services;

import searchengine.config.JsoupConnectConfig;
import searchengine.config.OtherSettings;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class PageHandler extends SiteHandler {
    private final String url;

    public PageHandler(Site site, String url, SiteRepository siteRepository, PageRepository pageRepository
            , LemmaRepository lemmaRepository, IndexRepository indexRepository
            , OtherSettings otherSettings, JsoupConnectConfig jsoupConnectConfig
            , IndexingService indexingService, LemmaFinder lemmaFinder) {
        super(site, siteRepository, pageRepository, lemmaRepository, indexRepository
                , otherSettings, jsoupConnectConfig, indexingService, lemmaFinder);
        this.url = url;
    }

    @Override
    protected SiteEntity getSiteEntity() {
        List<SiteEntity> siteEntityList = siteRepository.findByUrl(site.getUrl());
        for (SiteEntity siteEntity : siteEntityList) {
            return siteEntity;
        }

        SiteEntity siteEntity = createSiteEntity(site, StatusType.INDEXED);
        siteRepository.save(siteEntity);

        return siteEntity;
    }

    @Override
    public void run() {
        SiteEntity siteEntity = getSiteEntity();
        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        forkJoinPool.invoke(
                new GetterSiteMap(siteEntity, url, this
                        , siteRepository, pageRepository, lemmaRepository, indexRepository
                        , otherSettings, jsoupConnectConfig
                        , indexingService, lemmaFinder
                )
        );
        deletePageWithContentIsNull(siteEntity.getId());
    }

    @Override
    public boolean isError() {
        return false;
    }

    @Override
    public void setError(boolean error) {

    }

    @Override
    public boolean isOunPage() {
        return true;
    }
}
