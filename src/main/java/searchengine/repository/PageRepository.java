package searchengine.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import searchengine.model.Page;
import searchengine.model.Site;


import java.util.List;


@Repository
public interface PageRepository extends JpaRepository<Page, Long> {
    long countBySiteByPage(Site siteByPage);
    @Override
    void deleteAll();

    @Query(value = "SELECT p.id FROM Page p WHERE p.path LIKE :url%",
            nativeQuery = true)
    List<Integer> findPathByPage(@Param("url") String path);
    @Transactional
    @Modifying
    @Query(value = "delete from Page p where p.id=:idPage",
            nativeQuery = true)
    void deletePathByPage(@Param("idPage") Integer idPage);

}

