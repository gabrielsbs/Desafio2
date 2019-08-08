package ag.strider.server.repository;

import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;
import ag.strider.server.model.Task;

@Repository
public interface TaskRepository  extends JpaRepository<Task,Long> {

    
}