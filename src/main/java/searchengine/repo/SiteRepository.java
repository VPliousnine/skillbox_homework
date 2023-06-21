package searchengine.repo;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.SiteItem;

@Repository
public interface SiteRepository extends JpaRepository<SiteItem, Integer> {

    SiteItem findByUrl(String url);

    SiteItem findSiteById(int id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site SET Status_Time = now() WHERE id = ?", nativeQuery = true)
    void updateStatusTimeById(Integer id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site SET Status = 'INDEXED', Status_Time = now() WHERE id = ?", nativeQuery = true)
    void setStatusToIndexed(Integer id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site SET last_error = ?, Status_Time = now() WHERE id = ?", nativeQuery = true)
    void setLastError(String lastError, Integer id);

    @Modifying
    @Transactional
    @Query(value = "UPDATE Site SET Status = 'FAILED', last_error = ?, Status_Time = now() WHERE id = ?", nativeQuery = true)
    void setStatusToIndexFailed(String lastError, Integer id);
}
