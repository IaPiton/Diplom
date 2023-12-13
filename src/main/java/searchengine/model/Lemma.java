package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Collection;
import java.util.Objects;

@Entity
@Data
@Table(name = "lemma", indexes = @Index(columnList = "lemma"))

public class Lemma implements Comparable {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private int id;

    @ManyToOne()
    @JoinColumn(name = "site_id", referencedColumnName = "id", nullable = false)
    private Site siteByLemma;

    @OneToMany(mappedBy = "lemmaByIndex")
    private Collection<Indexes> indexesById;

    @Column(name = "lemma")
    private String lemma;
    private Integer frequency;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lemma lemma1 = (Lemma) o;
        return id == lemma1.id && Objects.equals(siteByLemma, lemma1.siteByLemma) && Objects.equals(indexesById, lemma1.indexesById) && Objects.equals(lemma, lemma1.lemma) && Objects.equals(frequency, lemma1.frequency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, siteByLemma, indexesById, lemma, frequency);
    }

    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
