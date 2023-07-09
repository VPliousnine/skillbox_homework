package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Page;

@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer>{

    @Query(value = "SELECT * FROM Lemma WHERE site_id = ?", nativeQuery = true)
    Page findBySiteId(int site_id);

    @Query(value = "SELECT COUNT(*) FROM lemma WHERE site_id = ?", nativeQuery = true)
    int getCountBySiteId(int siteId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO lemma (site_id, lemma, frequency) VALUES (?, ?, 1) ON DUPLICATE KEY UPDATE frequency = frequency + 1", nativeQuery = true)
    void saveLemma(int siteId, String lemma);

    @Query(value = "SELECT * FROM lemma WHERE site_id = ? AND lemma = ?", nativeQuery = true)
    Lemma getLemma(int siteId, String lemma);

    @Query(value = "SELECT COALESCE(SUM(frequency), 0) FROM lemma WHERE site_id = COALESCE(?, site_id) AND lemma = ?", nativeQuery = true)
    int getFrequency(Integer siteId, String lemma);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM lemma WHERE site_id=?", nativeQuery = true)
    void deleteBySite(int siteId);

    @Modifying
    @Transactional
    @Query(value = "UPDATE lemma SET frequency = frequency - 1 WHERE id IN (SELECT lemma_id FROM `index` WHERE page_id = ?)", nativeQuery = true)
    void deleteByPage(int siteId);

}