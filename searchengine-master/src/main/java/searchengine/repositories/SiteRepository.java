package searchengine.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import searchengine.model.SiteEntity;
import searchengine.model.StatusType;

import java.util.List;

public interface SiteRepository extends JpaRepository<SiteEntity, Integer> {
    List<SiteEntity> findByUrl(String url);
    List<SiteEntity> findByStatus(StatusType status);

    @Query(value = "SELECT s.* FROM site s INNER JOIN page p ON p.site_id = s.id WHERE p.id = ?", nativeQuery = true)
    List<SiteEntity> findByPageId(int pageId);
}
