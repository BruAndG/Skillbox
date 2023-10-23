package searchengine.services;

import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;

import java.time.LocalDateTime;

@Service
public class Utils {
    public String getRegexToFilterUrl(String url) {
        if (!url.startsWith("http")) {
            return null;
        }

        String[] str1 = url.split("/");
        if ((str1.length < 3) || (str1[2] == null)) {
            return null;
        }

        StringBuilder result = new StringBuilder("http[s]?://(www\\.)?");
        String[] str2 = str1[2].split("\\.");
        for (int i = 0; i < str2.length; i++) {
            if (!((i == 0) && str2[i].equals("www"))) {
                result.append(str2[i]);
                if (i != str2.length - 1) {
                    result.append(".");
                }
            }
        }
        result.append(".*");

        return result.toString();
    }

    public boolean isCorrectDomain(String url, String regex) {
        return ((regex == null) || url.matches(regex));
    }

    public SiteEntity createSiteEntity(Site site, StatusType statusType) {
        SiteEntity newSite = new SiteEntity();

        newSite.setName(site.getName());
        newSite.setUrl(site.getUrl());
        newSite.setStatus(statusType);
        newSite.setStatusTime(LocalDateTime.now());

        return newSite;
    }

}
