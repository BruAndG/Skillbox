package searchengine.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import searchengine.dto.indexing.IndexingResponse;
import searchengine.dto.indexing.IndexingResponseServ;
import searchengine.dto.searching.SearchingResponse;
import searchengine.dto.searching.SearchingResponseServ;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.dto.statistics.StatisticsResponseServ;
import searchengine.services.IndexingService;
import searchengine.services.SearchingService;
import searchengine.services.StatisticsService;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final StatisticsService statisticsService;
    private final IndexingService indexingService;
    private final SearchingService searchingService;

    public ApiController(StatisticsService statisticsService, IndexingService indexingService
            , SearchingService searchingService) {
        this.statisticsService = statisticsService;
        this.indexingService = indexingService;
        this.searchingService = searchingService;
    }

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        StatisticsResponseServ responseServ = statisticsService.getStatistics();
        return new ResponseEntity<>(responseServ.getStatisticsResponse(), responseServ.getStatus());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponseServ responseServ = indexingService.startIndexing();
        return new ResponseEntity<>(responseServ.getIndexingResponse(), responseServ.getStatus());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        IndexingResponseServ responseServ = indexingService.stopIndexing();
        return new ResponseEntity<>(responseServ.getIndexingResponse(), responseServ.getStatus());
    }

    @PostMapping("/indexPage")
    public ResponseEntity<IndexingResponse> indexingPage(String url) {
        IndexingResponseServ responseServ = indexingService.indexingPage(url);
        return new ResponseEntity<>(responseServ.getIndexingResponse(), responseServ.getStatus());
    }

    @GetMapping("/search")
    public ResponseEntity<SearchingResponse> search(String query, String site, Integer offset, Integer limit) {
        SearchingResponseServ responseServ = searchingService.search(query, site, offset, limit);
        return new ResponseEntity<>(responseServ.getSearchingResponse(), responseServ.getStatus());
    }

}
