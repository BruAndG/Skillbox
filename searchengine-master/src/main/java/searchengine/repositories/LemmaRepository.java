package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.LemmaEntity;

import javax.transaction.Transactional;
import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Long> {
    List<LemmaEntity> findByLemma(String lemma);

    @Query(value = "SELECT * FROM lemma l WHERE l.lemma = ? AND l.site_id = ?", nativeQuery = true)
    List<LemmaEntity> findByLemmaAndSiteId(String lemma, long siteId);

    @Query(value = "SELECT COUNT(*) FROM lemma l WHERE l.site_id = ?", nativeQuery = true)
    int countBySiteId(long siteId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE lemma l SET l.frequency = l.frequency + ? WHERE l.lemma = ? AND l.site_id = ?"
            , nativeQuery = true)
    int incFrequencyByLemmaAndSiteId(int inc, String lemma, long siteId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma l WHERE l.lemma = ? AND l.site_id = ? AND l.frequency <= 0"
            , nativeQuery = true)
    int deleteLemmaIsFrequencyEqualsZero(String lemma, long siteId);
}
