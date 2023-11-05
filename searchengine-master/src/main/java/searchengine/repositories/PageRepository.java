package searchengine.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Long> {
    @Query(value = "SELECT * FROM page p WHERE p.path = ? AND p.site_id = ?", nativeQuery = true)
    List<PageEntity> findByPathAndSiteId(String path, long siteId);

    @EntityGraph(value = "PageEntity.indexes")
    Optional<PageEntity> findById(long id);

    @Query(value = "SELECT COUNT(*) FROM page p WHERE p.site_id = ?", nativeQuery = true)
    int countBySiteId(long siteId);

    @Query(value = "SELECT * FROM page p WHERE p.site_id = ? AND p.content is null", nativeQuery = true)
    List<PageEntity> findBySiteIdAndContentIsNull(long siteId);

    @Query(value = "SELECT * FROM page p INNER JOIN `index` i ON i.page_id = p.id"
            + " INNER JOIN lemma l ON l.id = i.lemma_id WHERE l.lemma = ?", nativeQuery = true)
    List<PageEntity> findByLemma(String lemma);

    @Query(value = "SELECT COUNT(*) FROM page p INNER JOIN `index` i ON i.page_id = p.id"
            + " INNER JOIN lemma l ON l.id = i.lemma_id WHERE p.id = ? AND l.lemma = ?", nativeQuery = true)
    int countByIdAndLemma(long pageId, String lemma);

}
