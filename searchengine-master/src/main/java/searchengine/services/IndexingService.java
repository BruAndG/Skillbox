package searchengine.services;

import searchengine.dto.indexing.IndexingResponseServ;

public interface IndexingService {
    IndexingResponseServ startIndexing();

    IndexingResponseServ stopIndexing();

    IndexingResponseServ indexingPage(String url);

    boolean isStopIndexingProcess();
}
