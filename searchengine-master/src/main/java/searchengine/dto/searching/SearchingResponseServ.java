package searchengine.dto.searching;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class SearchingResponseServ {
    private SearchingResponse searchingResponse;
    private HttpStatus status;
}
