package searchengine.services;

import searchengine.dto.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    void startIndexing();

    SearchResponse search(String query, Integer offset, Integer limit, String site);
}
