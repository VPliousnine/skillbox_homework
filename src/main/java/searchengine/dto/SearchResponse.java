package searchengine.dto;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponse {
    private boolean result;
    private Integer count;
    private String message;
    private List<SearchItem> data;
}
