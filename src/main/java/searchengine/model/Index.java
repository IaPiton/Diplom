package searchengine.model;

import jakarta.persistence.*;
import lombok.Data;
@Entity
@Data
@Table(name = "index" )
public class Index {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private int id;
    private float rank;

}
