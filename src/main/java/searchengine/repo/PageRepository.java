package searchengine.repo;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import searchengine.model.Page;

@Repository
public interface PageRepository extends JpaRepository<Page, Integer> {

    Page findPageById(int id);

    @Query(value = "SELECT * FROM Page WHERE site_id = ? AND path=?", nativeQuery = true)
    Page findBySiteIdAndPath(int site_id, String path);

    @Query(value = "SELECT COUNT(*) FROM Page WHERE site_id = COALESCE(?, site_id)", nativeQuery = true)
    int getPageCountBySiteId(Integer siteId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM Page WHERE site_id=?", nativeQuery = true)
    void deleteBySite(int site_id);
}