package group1.com.MangaSystemAndManagement.repository;

import group1.com.MangaSystemAndManagement.model.ChapterComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChapterCommentRepository extends JpaRepository<ChapterComment, Long> {
    List<ChapterComment> findByChapterIdOrderByCreatedAtAsc(Long chapterId);
}