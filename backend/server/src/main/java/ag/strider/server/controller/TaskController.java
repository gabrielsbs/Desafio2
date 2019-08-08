package ag.strider.server.controller;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import ag.strider.server.repository.TaskRepository;
import ag.strider.server.model.Task;

@RestController
@RequestMapping({"/tasks"})
public class TaskController {

    private TaskRepository repository;

    @PersistenceContext
    EntityManager em;

    TaskController(TaskRepository repository){
        this.repository = repository;
    }

    @GetMapping
    public List<Task> getAll(){
        return repository.findAll();
    }

    @GetMapping({"/{id}"})
    public ResponseEntity<Task> getTask(@PathVariable Long id){
        return repository.findById(id)
          .map(record -> ResponseEntity.ok().body(record))
          .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping({"/pending"})
    public List<Task> getPendingTasks(){
        List<Task> tasks = repository.findAll();
        List<Task> pendingTasks = new ArrayList<Task>();
        for(int i = 0; i< tasks.size(); i++){
            if(tasks.get(i).getPhase().equals("Pendente"))
                pendingTasks.add(tasks.get(i));
        }
        return pendingTasks;
    }

    @PostMapping
    public Task newTask(@RequestBody Task task){
        return repository.save(task);
    }

    @PutMapping({"/{id}"})
    public ResponseEntity<Task> updateTask (@PathVariable("id") long id, @RequestBody Task task){
        return repository.findById(id)
        .map(record -> {
            record.setName(task.getName());
            record.setPhase(task.getPhase());
            record.setImageLocation(task.getImageLocation());
            Task updated = repository.save(record);
            return ResponseEntity.ok().body(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @RequestMapping(value = {"/taskDone/{id}"}, method = RequestMethod.PUT, consumes = {"multipart/form-data"} )
    public ResponseEntity<Task> taskSolution (@PathVariable("id") long id,  @RequestPart(value = "task") Task task, 
    @RequestPart(value = "image")  MultipartFile image){
        File file = new File("../frontend/src/Images/"+id+".jpeg");
        
        FileOutputStream fos;
        try {
            file.createNewFile();
            fos = new FileOutputStream(file);
            fos.write(image.getBytes());
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        BufferedImage bf;
        try {
            bf = ImageIO.read(file);
            int height = bf.getHeight();
            int width = bf.getWidth();
            BufferedImage src = new BufferedImage(height,width,bf.getType());
            Graphics2D graphics2D = src.createGraphics();
            graphics2D.translate((height - width) / 2, (height - width) / 2);
            graphics2D.rotate(Math.PI / 2, height / 2, width / 2);
            graphics2D.drawRenderedImage(bf, null);
            ImageIO.write(src, "jpeg", file);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
       

        task.setImageLocation("./Images/"+id+".jpeg");
        task.setPhase("Concluída");
        return repository.findById(id)
        .map(record -> {
            record.setName(task.getName());
            record.setPhase(task.getPhase());
            record.setImageLocation(task.getImageLocation());
            Task updated = repository.save(record);
            return ResponseEntity.ok().body(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping({"/{id}"})
    public ResponseEntity<?> removeTask(@PathVariable("id") long id){
        File f = new File("../frontend/src/Images/"+id+".jpeg");
        f.delete();
        return repository.findById(id)
        .map(record -> {
            repository.deleteById(id);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }
    
}