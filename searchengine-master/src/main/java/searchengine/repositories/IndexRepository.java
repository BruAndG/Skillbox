package searchengine.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.IndexEntity;

import java.util.Optional;

public interface IndexRepository extends JpaRepository<IndexEntity, Long> {
    @EntityGraph(value = "IndexEntity.lemmaEntity")
    Optional<IndexEntity> findById(long id);

    @Query(value = "SELECT SUM(i.`rank`) FROM `index` i INNER JOIN lemma l ON l.id = i.lemma_id"
            + " WHERE i.page_id = ? AND l.lemma = ?", nativeQuery = true)
    float getRankByPageAndLemma(long pageId, String lemma);
}
