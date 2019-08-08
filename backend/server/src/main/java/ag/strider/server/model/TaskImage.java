package ag.strider.server.model;

import java.awt.image.BufferedImage;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Data;


@NoArgsConstructor
@AllArgsConstructor
@Data
public class TaskImage {
    private Task task;
    private BufferedImage image;
    
}