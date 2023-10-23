package searchengine.dto.searching;

import lombok.Data;

import java.util.List;

@Data
public class SearchingResponse {
    private boolean result;
    private Integer count;
    private List<SearchingData> data;
    private String error;
}
