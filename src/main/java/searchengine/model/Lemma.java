package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Collection;


@Entity
@Data
@Table(name = "lemma", uniqueConstraints = {@UniqueConstraint(columnNames = {"lemma", "site_id"})})
public class Lemma {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
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
