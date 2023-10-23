package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.LemmaEntity;
import searchengine.model.PageEntity;

import java.util.List;

public interface LemmaRepository extends JpaRepository<LemmaEntity, Integer> {
    List<LemmaEntity> findByLemma(String lemma);

    @Query(value = "SELECT COUNT(*) FROM lemma l WHERE l.site_id = ?", nativeQuery = true)
    int countBySiteId(int site_id);

//    @Query(value = "SELECT * FROM lemma l WHERE l.site_id = ? ORDERED l.frequency", nativeQuery = true)
//    List<LemmaEntity> findBySiteIdOrderedFrequency(int site_id);

}
