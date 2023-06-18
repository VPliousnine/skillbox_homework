package searchengine.services;

import searchengine.dto.MessageResponse;
import searchengine.dto.SearchResponse;
import searchengine.dto.statistics.StatisticsResponse;

public interface StatisticsService {
    StatisticsResponse getStatistics();
    void startIndexing();

    MessageResponse indexPage(String url);

    SearchResponse search(String query, Integer offset, Integer limit, String site);
}
