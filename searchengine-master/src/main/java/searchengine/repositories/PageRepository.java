package searchengine.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import searchengine.model.PageEntity;
import searchengine.model.SiteEntity;

import java.util.List;
import java.util.Optional;

public interface PageRepository extends JpaRepository<PageEntity, Integer> {
    List<PageEntity> findByPath(String path);

    //@Query("SELECT p FROM page p LEFT JOIN FETCH p.indexes WHERE i.page_id = :p_page_id")
    //List<PageEntity> findByPageId(@Param("p_page_id") Integer pageId);

    @EntityGraph(value = "PageEntity.indexes")
    Optional<PageEntity> findById(Integer id);

    @Query(value = "SELECT COUNT(*) FROM page p WHERE p.site_id = ?", nativeQuery = true)
    int countBySiteId(int id);

//    @Query(value = "SELECT * FROM page p INNER JOIN `index` i ON i.page_id = p.id"
//            + " INNER JOIN lemma l ON l.id = i.lemma_id WHERE l.id = ?", nativeQuery = true)
//    List<PageEntity> findByLemmaId(int lemmaId);

    @Query(value = "SELECT * FROM page p INNER JOIN `index` i ON i.page_id = p.id"
            + " INNER JOIN lemma l ON l.id = i.lemma_id WHERE l.lemma = ?", nativeQuery = true)
    List<PageEntity> findByLemma(String lemma);

    @Query(value = "SELECT COUNT(*) FROM page p INNER JOIN `index` i ON i.page_id = p.id"
            + " INNER JOIN lemma l ON l.id = i.lemma_id WHERE p.id = ? AND l.lemma = ?", nativeQuery = true)
    int countByIdAndLemma(int id, String lemma);


    //@Modifying
    //@Query(value = "DELETE FROM page p WHERE p.site_id = :siteId", nativeQuery = true)
    //void deleteBySiteId(@Param("siteId") Integer siteId);

    //void deleteBySiteEntity(SiteEntity siteEntity);


}
