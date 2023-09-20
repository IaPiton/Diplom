package searchengine.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import searchengine.model.Indexes;

public interface IndexesRepository  extends JpaRepository<Indexes, Integer> {
}
