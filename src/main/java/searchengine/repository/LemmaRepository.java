package searchengine.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Indexes;
import searchengine.model.Lemma;
import searchengine.model.Site;

import java.util.List;


@Repository
public interface LemmaRepository extends JpaRepository<Lemma, Integer> {
    long countBySiteByLemma(Site siteByLemma);

    @Query(value = "SELECT sum(l.frequency) FROM Lemma l " +
            "where l.lemma = ?1 and site_id IN ?2"
            , nativeQuery = true)
    Integer frequencyLemma(@Param("lemma") String lemma,
                           @Param("siteId") List<Integer> siteId);

    boolean existsByLemmaAndSiteByLemma(String lemmas, Site site);


    @Transactional
    Lemma findFirstByLemmaAndSiteByLemma(String lemma, Site site);

    Lemma findLemmaByIndexesById(Indexes indexes);
}
