package searchengine.model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;


@Entity
@Data
@Table(name = "site")
public class Site {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
            generator = "site_sequence")
    @SequenceGenerator(name = "site_sequence")
    private int id;
    @Enumerated(EnumType.STRING)
    private Status status;
    @Column(name = "status_time")
    private LocalDateTime statusTime;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(unique = true, columnDefinition = "VARCHAR(255)")
    private String url;
    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @OneToMany(mappedBy = "siteByPage")
    private Collection<Page> pagesById;

    @OneToMany(mappedBy = "siteByLemma")
    private Collection<Lemma> lemmasById;
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Site site = (Site) o;
        return id == site.id && status == site.status && Objects.equals(statusTime, site.statusTime)
                && Objects.equals(lastError, site.lastError) && Objects.equals(url, site.url)
                && Objects.equals(name, site.name);
    }
    @Override
    public int hashCode() {
        return Objects.hash(id, status, statusTime, lastError, url, name);
    }
}
