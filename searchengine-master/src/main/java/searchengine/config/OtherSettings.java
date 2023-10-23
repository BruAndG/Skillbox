package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "other-settings")
public class OtherSettings {
    private Integer pauseBeforeRequest;
    private Integer minPercentagePagesToExcludeLemmas;
    private Integer maxWordsBefore = 15;
    private Integer maxWordsAfter = 15;
    private String errorMessageIndexingStoppedByUser;
    private String errorMessageIsAlreadyStarted;
    private String errorMessageIsNotRunning;
    private String errorPageLocatedOutsideSites;
    private String errorMessageEmptySearchQuery;
}
