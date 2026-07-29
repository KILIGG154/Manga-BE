package group1.com.MangaSystemAndManagement.service.impl;
import group1.com.MangaSystemAndManagement.dto.request.ChapterRequest;
import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.repository.ChapterRepository;
import group1.com.MangaSystemAndManagement.service.interfaces.ChapterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;
@Service
@RequiredArgsConstructor
public class ChapterServiceImpl implements ChapterService {
    private final ChapterRepository repository;
    @Override
    @Transactional
    public Chapter create(ChapterRequest request) {
        Chapter entity = new Chapter();
        org.springframework.beans.BeanUtils.copyProperties(request, entity);
        return repository.save(entity);
    }
    @Override
    public Optional<Chapter> findById(Long id) {
        return repository.findById(id);
    }
    @Override
    public List<Chapter> findAll() {
        return repository.findAll();
    }
    @Override
    @Transactional
    public Chapter update(Long id, ChapterRequest request) {
        Chapter entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Chapter not found with id " + id));
        org.springframework.beans.BeanUtils.copyProperties(request, entity);
        return repository.save(entity);
    }
    @Override
    @Transactional
    public Chapter updateOverdueStatus(Long id) {
        Chapter chapter = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Chapter not found with id " + id));
            
        java.time.LocalDate today = java.time.LocalDate.now();
        java.time.Instant now = java.time.Instant.now();
        
        // If today < deadline date, do not change anything
        if (chapter.getDeadline() != null && now.isBefore(chapter.getDeadline())) {
            return chapter;
        }
        
        if (chapter.getChapterStatus() != group1.com.MangaSystemAndManagement.model.ChapterStatus.COMPLETED &&
            chapter.getChapterStatus() != group1.com.MangaSystemAndManagement.model.ChapterStatus.PUBLISHED &&
            chapter.getChapterStatus() != group1.com.MangaSystemAndManagement.model.ChapterStatus.SCHEDULED &&
            chapter.getChapterStatus() != group1.com.MangaSystemAndManagement.model.ChapterStatus.OVERDUE) {
            
            boolean isOverdue = false;
            if (chapter.getEndDate() != null && today.isAfter(chapter.getEndDate())) {
                isOverdue = true;
            } else if (chapter.getDeadline() != null && now.isAfter(chapter.getDeadline())) {
                isOverdue = true;
            }
            
            if (isOverdue) {
                chapter.setChapterStatus(group1.com.MangaSystemAndManagement.model.ChapterStatus.OVERDUE);
                return repository.save(chapter);
            }
        }
        return chapter;
    }
    @Override
    @Transactional
    public Chapter updateStatusCompleted(Long id) {
        Chapter entity = repository.findById(id)
            .orElseThrow(() -> new RuntimeException("Chapter not found with id " + id));
        entity.setChapterStatus(group1.com.MangaSystemAndManagement.model.ChapterStatus.COMPLETED);
        return repository.save(entity);
    }
    @Override
    @Transactional
    public void publishChaptersByPlanId(Long planId) {
        List<Chapter> chapters = repository.findByProductionPlanId(planId);
        java.time.LocalDate today = java.time.LocalDate.now();
        for (Chapter chapter : chapters) {
            if (chapter.getChapterStatus() == group1.com.MangaSystemAndManagement.model.ChapterStatus.COMPLETED) {
                java.time.LocalDate publishDate = chapter.getPublishDate();
                if (publishDate == null && chapter.getProductionPlan() != null) {
                    publishDate = chapter.getProductionPlan().getPublishDate();
                }
                if (publishDate != null && publishDate.equals(today)) {
                    chapter.setChapterStatus(group1.com.MangaSystemAndManagement.model.ChapterStatus.PUBLISHED);
                    repository.save(chapter);
                }
            }
        }
    }
    @Override
    public List<Chapter> findPublishedChapters() {
        return repository.findByChapterStatus(group1.com.MangaSystemAndManagement.model.ChapterStatus.PUBLISHED);
    }
    @Override
    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new RuntimeException("Chapter not found with id " + id);
        }
        repository.deleteById(id);
    }
}
