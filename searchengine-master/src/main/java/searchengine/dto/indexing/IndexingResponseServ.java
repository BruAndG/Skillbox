package searchengine.dto.indexing;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class IndexingResponseServ {
    private IndexingResponse indexingResponse;
    private HttpStatus status;
}
