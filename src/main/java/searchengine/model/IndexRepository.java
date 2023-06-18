package searchengine.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Repository
public interface IndexRepository extends JpaRepository<Index, Integer>{

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index` WHERE page_id IN (SELECT id FROM page WHERE site_id = ?)", nativeQuery = true)
    void deleteBySite(int siteId);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM `index` WHERE page_id = ?", nativeQuery = true)
    void deleteByPage(int pageId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO `index` (page_id, lemma_id, `rank`) VALUES (?, ?, ?)", nativeQuery = true)
    void saveIndex(int pageId, int lemmaId, float rank);

    @Query(value = "SELECT * FROM `index` WHERE lemma_id IN (SELECT lemma.id FROM lemma WHERE site_id = COALESCE(?, site_id) AND lemma = ?)", nativeQuery = true)
    Set<Index> getPages(Integer siteId, String lemma);
}
