package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
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

import javax.transaction.Transactional;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.RecursiveAction;

public class GetterSiteMap extends RecursiveAction {
    private final SiteEntity rootSiteEntity;
    private final String nodeUrl;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;
    private final LemmaRepository lemmaRepository;
    private final IndexRepository indexRepository;
    private final OtherSettings otherSettings;
    private final JsoupConnectConfig jsoupConnectConfig;
    private final IndexingService indexingService;
    private final StartHandler handler;
    private final LemmaFinder lemmaFinder;
    private final Utils utils;

    public GetterSiteMap(SiteEntity rootSiteEntity, String nodeUrl
            , SiteRepository siteRepository, PageRepository pageRepository, LemmaRepository lemmaRepository
            , IndexRepository indexRepository
            , JsoupConnectConfig jsoupConnectConfig, OtherSettings otherSettings
            , IndexingService indexingService, StartHandler handler, LemmaFinder lemmaFinder, Utils utils) {
        this.rootSiteEntity = rootSiteEntity;
        this.nodeUrl = nodeUrl;
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.lemmaRepository = lemmaRepository;
        this.indexRepository = indexRepository;
        this.jsoupConnectConfig = jsoupConnectConfig;
        this.otherSettings = otherSettings;
        this.indexingService = indexingService;
        this.handler = handler;
        this.lemmaFinder = lemmaFinder;
        this.utils = utils;
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

    private String getPathFromUrl(String url) {
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

    private PageEntity getPageEntity(String path) {
        List<PageEntity> pageEntityList = pageRepository.findByPath(path);
        for (PageEntity pageEntity : pageEntityList) {
            if (pageEntity.getSiteEntity().getId() == rootSiteEntity.getId()) {
                return pageEntity;
            }
        }
        return null;
    }

    private Connection.Response loadHtml(String url) {

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

    private PageEntity createPageEntity(SiteEntity siteEntity, String path, int code, String content) {
        PageEntity newPage = new PageEntity();

        newPage.setSiteEntity(siteEntity);
        newPage.setPath(path);
        newPage.setCode(code);
        newPage.setContent(content);

        return newPage;
    }

    private void savePage(PageEntity pageEntity) {
        rootSiteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(rootSiteEntity);
        pageRepository.save(pageEntity);
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
                int frequency = lemmaEntity.getFrequency() - 1;
                if (frequency > 0) {
                    lemmaEntity.setFrequency(frequency);
                } else {
                    lemmaRepository.delete(lemmaEntity);
                }
                lemmaRepository.save(lemmaEntity);
            }
        }

    }

    private void deletePage(PageEntity pageEntity) {
        compensationFrequency(pageEntity);
        pageRepository.delete(pageEntity);
    }

    private LemmaEntity findByLemma(String lemmaText) {
        List<LemmaEntity> lemmaList = lemmaRepository.findByLemma(lemmaText);
        for (LemmaEntity lemmaEntity : lemmaList) {
            if (lemmaEntity.getSiteEntity().getId() == rootSiteEntity.getId()) {
                return lemmaEntity;
            }
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

    private IndexEntity createIndexEntity(PageEntity pageEntity, LemmaEntity lemmaEntity, float rank) {
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
            } else {
                lemmaEntity.setFrequency(lemmaEntity.getFrequency() + 1);
            }
            lemmaRepository.save(lemmaEntity);

            IndexEntity indexEntity = createIndexEntity(pageEntity, lemmaEntity, lemma.getValue());
            indexRepository.save(indexEntity);

        }
    }

    @Transactional
    private Document getAndSaveHtmlBody() {
        Document document = null;
        Connection.Response response;
        PageEntity newPage;
        String path = getPathFromUrl(nodeUrl);

        synchronized (rootSiteEntity) {

            PageEntity pageEntity = getPageEntity(path);
            if (pageEntity != null) {
                if (handler.isOunPage()) {
                    synchronized (pageEntity) {
                        deletePage(pageEntity);
                    }
                } else {
                    return null;
                }
            }
            response = loadHtml(nodeUrl);
            if (response == null) {
                return null;
            }

            String content = response.body();
            int code = response.statusCode();
            newPage = createPageEntity(rootSiteEntity, path, code, content);
            savePage(newPage);
        }

        try {
            document = response.parse();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // индексация страницы
        synchronized (newPage) {
            parseTextToLemmas(document.text(), newPage);
        }

        return document;
    }

    private String transformUrl(String url) {
        if (!url.endsWith("/")) {
            return url + "/";
        }
        return url;
    }

    /*
    Метод ищет URL в переданном списке
     */
    private boolean isExistsInChild(List<String> nodes, String url) {
        if (nodes != null) {
            String transUrl = transformUrl(url);
            for (String child : nodes) {
                if (transformUrl(child).equals(transUrl)) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
    Метод формирует список URL страницы (с фильтром по домену)
     */
    private List<String> getChildNodes(Document document) {
        List<String> result = new ArrayList<>();

        String regex = utils.getRegexToFilterUrl(nodeUrl);

        Elements elements = document.select("a[href]");
        for (Element element : elements) {
            String url = element.absUrl("href");

            if (utils.isCorrectDomain(url, regex) && !isExistsInChild(result, url)) {
                result.add(url);
            }
        }

        return result;
    }

    @Override
    protected void compute() {

        try {
            if (indexingService.isStopIndexingProcess() || handler.isError()) {
                return;
            }

            Document document = getAndSaveHtmlBody();
            if (document == null) {
                return;
            }

            // если индексируем только одну страницу, то выходим
            if (handler.isOunPage()) {
                return;
            }

            List<String> childNodes = getChildNodes(document);
            List<RecursiveAction> forks = new ArrayList<>();
            for (String childNode : childNodes) {
                GetterSiteMap subTask =
                        new GetterSiteMap(rootSiteEntity, childNode
                                , siteRepository, pageRepository, lemmaRepository, indexRepository
                                , jsoupConnectConfig, otherSettings
                                , indexingService, handler, lemmaFinder, utils
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
