package group1.com.MangaSystemAndManagement.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Nationalized;

import java.time.Instant;

/**
 * Decision Log 2026-07-27 §AI-05 + §AI-12:
 * Comment trao đổi trên Chapter — dùng khi Chapter bị Return/Recall để Trao đổi
 * giữa Tantou/Board/Team về Task cần sửa.
 */
@Getter
@Setter
@Entity
@Table(name = "ChapterComment")
public class ChapterComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "Id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ChapterId", nullable = false)
    @JsonIgnore
    private Chapter chapter;

    @Column(name = "AuthorId", nullable = false)
    private Long authorId;

    @Nationalized
    @Column(name = "AuthorName", length = 255)
    private String authorName;

    @Nationalized
    @Column(name = "Body", nullable = false, columnDefinition = "nvarchar(max)")
    private String body;

    @Column(name = "CreatedAt", nullable = false)
    private Instant createdAt = Instant.now();
}