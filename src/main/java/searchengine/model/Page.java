package searchengine.model;

import jakarta.persistence.*;
import jakarta.persistence.Index;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "Page", uniqueConstraints = {
        @UniqueConstraint(name = "site_path", columnNames = {"site_id", "path"})})
public class Page {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    @Column(name = "site_id", nullable = false)
    private int siteId;
    @Column(nullable = false, columnDefinition = "TEXT")
    private String path;
    @Column(nullable = false)
    private int code;
    @Column(nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;
}
