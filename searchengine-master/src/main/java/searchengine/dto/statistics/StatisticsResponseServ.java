package searchengine.dto.statistics;

import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
public class StatisticsResponseServ {
    private StatisticsResponse statisticsResponse;
    private HttpStatus status;
}
