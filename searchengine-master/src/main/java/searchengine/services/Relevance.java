package searchengine.services;

import lombok.Getter;
import lombok.Setter;
import searchengine.model.PageEntity;

@Getter
@Setter
public class Relevance {
    private final PageEntity pageEntity;
    private float absoluteRelevance = 0;
    private float relativeRelevance = 0;

    public Relevance(PageEntity pageEntity) {
        this.pageEntity = pageEntity;
    }
}
