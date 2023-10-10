package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    Lemma findByIdAndSiteByLemma(int id, Site siteByLemma);
    long countBySiteByLemma(Site siteByLemma);
    @Override
    @Modifying
    @Query("DELETE FROM Lemma")
    void deleteAll();
    @Transactional
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE lemma  SET frequency = frequency - 1 " +
            "where id in (select lemma_id from index_page where page_id = ?);", nativeQuery = true)
    void updateLemmaFrequency(int pageId);
    @Modifying
    @Query("DELETE FROM Lemma l WHERE l.frequency=0")
    void deleteOfFrequencyIsNull();
    @Modifying
    @Query(value = "delete from Lemma l where l.id=:id",
            nativeQuery = true)
    void deleteLemmaPathByPage( @Param("id") Integer id);

    List<Lemma> findByLemmaInAndSiteByLemmaOrderByFrequency(List<String> lemmas, Site siteByLemma);
}
