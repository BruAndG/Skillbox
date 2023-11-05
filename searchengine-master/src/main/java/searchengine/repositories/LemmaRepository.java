package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.LemmaEntity;

import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    List<LemmaEntity> findByLemma(String lemma);

    @Query(value = "SELECT * FROM lemma l WHERE l.lemma = ? AND l.site_id = ?", nativeQuery = true)
    List<LemmaEntity> findByLemmaAndSiteId(String lemma, long siteId);

    @Query(value = "SELECT COUNT(*) FROM lemma l WHERE l.site_id = ?", nativeQuery = true)
    int countBySiteId(long siteId);

}
