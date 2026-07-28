package group1.com.MangaSystemAndManagement.repository;
import group1.com.MangaSystemAndManagement.model.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByTantouId(Long tantouId);
}
