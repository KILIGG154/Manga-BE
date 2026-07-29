package group1.com.MangaSystemAndManagement.controller;
import group1.com.MangaSystemAndManagement.dto.request.ChapterRequest;
import group1.com.MangaSystemAndManagement.dto.response.ChapterResponse;
import group1.com.MangaSystemAndManagement.model.Chapter;
import group1.com.MangaSystemAndManagement.service.interfaces.ChapterService;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/chapters")
@Tag(name = "Chapter", description = "Chapter management APIs")
@RequiredArgsConstructor
public class ChapterController {
    private final ChapterService service;
    @PostMapping
    public ResponseEntity<ResponseBase> create(@RequestBody ChapterRequest request) {
        try {
            Chapter result = service.create(request);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Created successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @GetMapping
    public ResponseEntity<ResponseBase> findAll() {
        try {
            List<Chapter> result = service.findAll();
            List<ChapterResponse> response = result.stream()
                    .map(ChapterResponse::from)
                    .toList();
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
    @GetMapping("/{id}")
    public ResponseEntity<ResponseBase> findById(@PathVariable Long id) {
        try {
            return service.findById(id)
                    .map(result -> ResponseEntity.status(200).body(new ResponseBase(200, "Success", result)))
                    .orElseGet(() -> ResponseEntity.status(404).body(new ResponseBase(404, "Not found", null)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<ResponseBase> update(@PathVariable Long id, @RequestBody ChapterRequest request) {
        try {
            Chapter result = service.update(id, request);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Updated successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @DeleteMapping("/{id}")
    public ResponseEntity<ResponseBase> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @PutMapping("/{id}/update-overdue")
    public ResponseEntity<ResponseBase> updateOverdueStatus(@PathVariable Long id) {
        try {
            Chapter result = service.updateOverdueStatus(id);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Updated overdue status successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
    @PutMapping("/{id}/completed")
    public ResponseEntity<ResponseBase> updateStatusCompleted(@PathVariable Long id) {
        try {
            Chapter result = service.updateStatusCompleted(id);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Updated status to COMPLETED successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @PutMapping("/publish-by-plan/{planId}")
    public ResponseEntity<ResponseBase> publishChaptersByPlanId(@PathVariable Long planId) {
        try {
            service.publishChaptersByPlanId(planId);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Published eligible chapters successfully", null));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
    @GetMapping("/published")
    public ResponseEntity<ResponseBase> getPublishedChapters() {
        try {
            List<Chapter> result = service.findPublishedChapters();
            List<ChapterResponse> response = result.stream()
                    .map(ChapterResponse::from)
                    .toList();
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", response));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }
}
