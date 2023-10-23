package searchengine.services;

import searchengine.dto.searching.SearchingResponseServ;

public interface SearchingService {
    SearchingResponseServ search(String query, String site, Integer offset, Integer limit);
}
