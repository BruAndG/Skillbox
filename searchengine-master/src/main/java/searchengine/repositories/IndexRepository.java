package searchengine.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.IndexEntity;
import searchengine.model.PageEntity;

import java.util.List;
import java.util.Optional;

public interface IndexRepository extends JpaRepository<IndexEntity, Integer> {

    //@Query(value = "SELECT i FROM `index` i WHERE i.page_id = :p_page_id", nativeQuery = true)
    //List<IndexEntity> findByPageId(@Param("p_page_id") Integer pageId);

    @EntityGraph(value = "IndexEntity.lemmaEntity")
    Optional<IndexEntity> findById(Integer id);

    @Query(value = "SELECT SUM(i.`rank`) FROM `index` i INNER JOIN lemma l ON l.id = i.lemma_id"
            + " WHERE i.page_id = ? AND l.lemma = ?", nativeQuery = true)
    float getRankByPageAndLemma(int pageId, String lemma);

}
