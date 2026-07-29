package group1.com.MangaSystemAndManagement.service.interfaces;
import group1.com.MangaSystemAndManagement.dto.request.ChapterRequest;
import group1.com.MangaSystemAndManagement.model.Chapter;
import java.util.List;
import java.util.Optional;
public interface ChapterService {
    Chapter create(ChapterRequest request);
    Optional<Chapter> findById(Long id);
    List<Chapter> findAll();
    Chapter update(Long id, ChapterRequest request);
    Chapter updateOverdueStatus(Long id);
    Chapter updateStatusCompleted(Long id);
    void publishChaptersByPlanId(Long planId);
    List<Chapter> findPublishedChapters();
    void delete(Long id);
}
