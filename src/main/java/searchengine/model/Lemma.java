package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Collection;
import java.util.Objects;

@Entity
@Data
@Table(name = "lemma", indexes = @Index(columnList = "lemma"))

public class Lemma {
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




}
