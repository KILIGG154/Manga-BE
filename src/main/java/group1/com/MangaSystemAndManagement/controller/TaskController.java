package group1.com.MangaSystemAndManagement.controller;
import group1.com.MangaSystemAndManagement.dto.request.TaskRequest;
import group1.com.MangaSystemAndManagement.model.Task;
import group1.com.MangaSystemAndManagement.service.interfaces.TaskService;
import group1.com.MangaSystemAndManagement.dto.response.ResponseBase;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/tasks")
@Tag(name = "Task", description = "Task management APIs")
@RequiredArgsConstructor
public class TaskController {
    private final TaskService service;
    @PostMapping
    public ResponseEntity<ResponseBase> create(@RequestBody TaskRequest request) {
        try {
            Task result = service.create(request);
            return ResponseEntity.status(201).body(new ResponseBase(201, "Created successfully", result));
        } catch (Exception e) {
            return ResponseEntity.status(409).body(new ResponseBase(409, e.getMessage(), null));
        }
    }
    @GetMapping
    public ResponseEntity<ResponseBase> findAll() {
        try {
            List<Task> result = service.findAll();
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", result));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ResponseBase(500, e.getMessage(), null));
        }
    }

    @GetMapping("/mangaka/{mangakaId}")
    public ResponseEntity<ResponseBase> getTasksByMangakaId(@PathVariable Long mangakaId) {
        try {
            List<Task> result = service.findByAssigneeId(mangakaId);
            return ResponseEntity.status(200).body(new ResponseBase(200, "Success", result));
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
    public ResponseEntity<ResponseBase> update(@PathVariable Long id, @RequestBody TaskRequest request) {
        try {
            Task result = service.update(id, request);
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
}
