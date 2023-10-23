package searchengine.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "jsoup-connect-settings")
public class JsoupConnectConfig {
    private String userAgent;
    private String referrer;
    private Integer timeout;
}
