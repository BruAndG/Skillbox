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

import javax.transaction.Transactional;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

public class PageHandler implements Runnable, StartHandler {
    private final Site rootSite;
    private final String urlPage;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final JsoupConnectConfig jsoupConnectConfig;
    private final OtherSettings otherSettings;
    private final IndexingService indexingService;
    private final Utils utils;
    private final LemmaFinder lemmaFinder;

    public PageHandler(Site rootSite, String urlPage
            , SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository
            , IndexRepository indexRepository
            , JsoupConnectConfig jsoupConnectConfig, OtherSettings otherSettings, IndexingService indexingService
            , Utils utils, LemmaFinder lemmaFinder) {
        this.rootSite = rootSite;
        this.urlPage = urlPage;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.jsoupConnectConfig = jsoupConnectConfig;
        this.otherSettings = otherSettings;
        this.indexingService = indexingService;
        this.utils = utils;
        this.lemmaFinder = lemmaFinder;
    }

    @Transactional
    private SiteEntity getSiteInDB(Site site) {

        SiteEntity result;

        List<SiteEntity> siteEntityList = siteRepository.findByUrl(site.getUrl());
        if ((siteEntityList == null) || (siteEntityList.size() == 0)) {
            result = utils.createSiteEntity(site, StatusType.INDEXED);
            siteRepository.save(result);
        } else {
            result = siteEntityList.get(0);
        }

        return result;
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

    @Override
    public void run() {
        SiteEntity rootSiteEntity = getSiteInDB(rootSite);

        ForkJoinPool forkJoinPool = ForkJoinPool.commonPool();
        forkJoinPool.invoke(
                new GetterSiteMap(
                        rootSiteEntity, urlPage
                        , siteRepository, pageRepository, lemmaRepository, indexRepository
                        , jsoupConnectConfig, otherSettings
                        , indexingService, this, lemmaFinder, utils
                )
        );
    }
}
