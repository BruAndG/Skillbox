package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.dao.DataIntegrityViolationException;
import searchengine.config.JsoupConnectConfig;
import searchengine.config.OtherSettings;
import searchengine.model.IndexEntity;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;
import searchengine.repositories.IndexRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class GetterSiteMap extends RecursiveAction {
    private final SiteEntity rootSiteEntity;
    private final String nodeUrl;
    private final StartHandler handler;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final OtherSettings otherSettings;
    private final JsoupConnectConfig jsoupConnectConfig;
    private final IndexingService indexingService;
    private final LemmaFinder lemmaFinder;

    public GetterSiteMap(SiteEntity rootSiteEntity, String nodeUrl, StartHandler handler
            , SiteRepository siteRepository, PageRepository pageRepository
            , LemmaRepository lemmaRepository, IndexRepository indexRepository
            , OtherSettings otherSettings, JsoupConnectConfig jsoupConnectConfig
            , IndexingService indexingService, LemmaFinder lemmaFinder) {
        this.rootSiteEntity = rootSiteEntity;
        this.nodeUrl = nodeUrl;
        this.handler = handler;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.otherSettings = otherSettings;
        this.jsoupConnectConfig = jsoupConnectConfig;
        this.indexingService = indexingService;
        this.lemmaFinder = lemmaFinder;
    }

    private String getPathFromUrl(String url) {
        if (url == null) {
            return "/";
        }

        if (url.startsWith(rootSiteEntity.getUrl())) {
            String result = url.substring(rootSiteEntity.getUrl().length());
            if (!result.startsWith("/")) {
                result = "/" + result;
            }
            return result;
        }

        int index = 0;
        for (int i = 0; i < 3; i++) {
            index = url.indexOf('/', index);
            if (index == -1) {
                break;
            }
            index++;
        }
        if (index > -1) {
            return url.substring(--index);
        }
        return "/";
    }

    private PageEntity findPageEntity(String path) {
        List<PageEntity> pageEntityList = pageRepository.findByPathAndSiteId(path, rootSiteEntity.getId());
        for (PageEntity pageEntity : pageEntityList) {
            return pageEntity;
        }

        return null;
    }

    private PageEntity createPageEntity(SiteEntity siteEntity, String path) {
        PageEntity newPage = new PageEntity();

        newPage.setSiteEntity(siteEntity);
        newPage.setPath(path);

        return newPage;
    }

    private void compensationFrequency(PageEntity pageEntity) {
        Optional<PageEntity> optionalPageEntity = pageRepository.findById(pageEntity.getId());
        if (!optionalPageEntity.isPresent()) {
            return;
        }
        PageEntity refreshPageEntity = optionalPageEntity.get();
        Set<IndexEntity> indexSet = refreshPageEntity.getIndexes();
        for (IndexEntity indexEntity : indexSet) {
            Optional<IndexEntity> optionalIndexEntity = indexRepository.findById(indexEntity.getId());
            if (optionalIndexEntity.isPresent()) {
                IndexEntity refreshIndexEntity = optionalIndexEntity.get();
                LemmaEntity lemmaEntity = refreshIndexEntity.getLemmaEntity();
                lemmaRepository.incFrequencyByLemmaAndSiteId(-1, lemmaEntity.getLemma(), rootSiteEntity.getId());
                lemmaRepository.deleteLemmaIsFrequencyEqualsZero(lemmaEntity.getLemma(), rootSiteEntity.getId());
            }
        }
    }

    private PageEntity getPageEntity() {
        String path = getPathFromUrl(nodeUrl);
        PageEntity pageEntity = findPageEntity(path);
        if (pageEntity != null) {
            if (handler.isOunPage()) {
                compensationFrequency(pageEntity);
                pageRepository.delete(pageEntity);
            } else {
                return null;
            }
        }

        PageEntity newPage = createPageEntity(rootSiteEntity, path);
        try {
            pageRepository.save(newPage);
        } catch (DataIntegrityViolationException e) {
            return null;
        }

        return newPage;
    }

    private String transformUrl(String url) {
        if (url == null) {
            return null;
        }
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

    private Connection jsoupConnection() {

        Connection connection = Jsoup.newSession();

        String userAgent = jsoupConnectConfig.getUserAgent();
        if (userAgent != null) {
            connection = connection.userAgent(userAgent);
        }

        String referrer = jsoupConnectConfig.getReferrer();
        if (referrer != null) {
            connection = connection.referrer(referrer);
        }

        Integer timeout = jsoupConnectConfig.getTimeout();
        if (timeout != null) {
            connection = connection.timeout(timeout);
        }

        return connection;
    }

    private Connection.Response loadHtml(String url) {

        if (url == null) {
            return null;
        }

        Integer pauseBeforeRequest = otherSettings.getPauseBeforeRequest();
        if (pauseBeforeRequest != null) {
            try {
                Thread.sleep(pauseBeforeRequest);
            } catch (InterruptedException e) {
                return null;
            }
        }

        try {
            Connection jsoupConnection = jsoupConnection();
            Connection.Response response = jsoupConnection.url(url).execute();
            return response;
        } catch (IOException e) {
            if (transformUrl(url).equals(transformUrl(rootSiteEntity.getUrl()))) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

    private Document getAndSaveContent(PageEntity pageEntity) {
        Connection.Response response = loadHtml(nodeUrl);

        if (response == null) {
            return null;
        }

        String content = response.body();
        int code = response.statusCode();

        pageEntity.setCode(code);
        pageEntity.setContent(content);
        pageRepository.save(pageEntity);

        //rootSiteEntity.setStatusTime(LocalDateTime.now());
        //siteRepository.save(rootSiteEntity);

        Document document = null;
        try {
            document = response.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return document;
    }

    private boolean isExistsInChild(List<String> nodes, String url) {

        if (url == null) {
            return true;
        }

        if (nodes != null) {
            String transUrl = transformUrl(url);
            for (String child : nodes) {
                if ((child != null) && transformUrl(child).equals(transUrl)) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> getChildNodes(Document document) {
        List<String> result = new ArrayList<>();

        String regex = Utils.getRegexToFilterUrl(nodeUrl);

        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String url = element.absUrl("href");

            if ((url != null) && !url.contains("#") && !Utils.isFile(url)
                    && Utils.isCorrectDomain(url, regex) && !isExistsInChild(result, url)) {
                result.add(url);
            }
        }

        return result;
    }

    private LemmaEntity findByLemma(String lemmaText) {
        List<LemmaEntity> lemmaList = lemmaRepository.findByLemmaAndSiteId(lemmaText, rootSiteEntity.getId());
        for (LemmaEntity lemmaEntity : lemmaList) {
            return lemmaEntity;
        }

        return null;
    }

    private LemmaEntity createLemmaEntity(SiteEntity siteEntity, String lemmaText) {
        LemmaEntity newLemma = new LemmaEntity();
        newLemma.setSiteEntity(siteEntity);
        newLemma.setLemma(lemmaText);
        newLemma.setFrequency(1);
        return newLemma;
    }

    private IndexEntity createIndexEntity(PageEntity pageEntity, LemmaEntity lemmaEntity, int rank) {
        IndexEntity newIndex = new IndexEntity();
        newIndex.setPageEntity(pageEntity);
        newIndex.setLemmaEntity(lemmaEntity);
        newIndex.setRank(rank);
        return newIndex;
    }

    private void parseTextToLemmas(String text, PageEntity pageEntity) {
        Map<String, Integer> lemmas = lemmaFinder.collectLemmas(text);
        for (Map.Entry<String, Integer> lemma : lemmas.entrySet()) {
            LemmaEntity lemmaEntity = findByLemma(lemma.getKey());
            if (lemmaEntity == null) {
                lemmaEntity = createLemmaEntity(rootSiteEntity, lemma.getKey());
                try {
                    lemmaRepository.save(lemmaEntity);
                } catch (DataIntegrityViolationException e) {
                    lemmaRepository.incFrequencyByLemmaAndSiteId(1, lemma.getKey(), rootSiteEntity.getId());
                    lemmaEntity = findByLemma(lemma.getKey());
                }
            } else {
                lemmaRepository.incFrequencyByLemmaAndSiteId(1, lemma.getKey(), rootSiteEntity.getId());
            }

            IndexEntity indexEntity = createIndexEntity(pageEntity, lemmaEntity, lemma.getValue());
            indexRepository.save(indexEntity);
        }
    }

    @Override
    protected void compute() {
        if (indexingService.isStopIndexingProcess() || handler.isError()) {
            return;
        }

        try {
            PageEntity pageEntity = getPageEntity();
            if (pageEntity == null) {
                return;
            }

            Document document = getAndSaveContent(pageEntity);
            if (document == null) {
                return;
            }

            parseTextToLemmas(document.text(), pageEntity);

            // если индексируем только одну страницу, то выходим
            if (handler.isOunPage()) {
                return;
            }

            List<String> childNodes = getChildNodes(document);
            List<RecursiveAction> forks = new ArrayList<>();
            for (String childNode : childNodes) {
                GetterSiteMap subTask =
                        new GetterSiteMap(rootSiteEntity, childNode, handler
                                , siteRepository, pageRepository, lemmaRepository, indexRepository
                                , otherSettings, jsoupConnectConfig
                                , indexingService, lemmaFinder
                        );
                subTask.fork();
                forks.add(subTask);
            }
            for (RecursiveAction subTask : forks) {
                subTask.join();
            }

        } catch (Exception e) {
            handler.setError(true);
            throw new RuntimeException(e);
        }

    }
}
