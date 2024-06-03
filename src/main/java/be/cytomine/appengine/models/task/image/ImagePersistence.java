package be.cytomine.appengine.models.task.image;

import be.cytomine.appengine.models.task.TypePersistence;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "image_type_persistence")
@Data
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class ImagePersistence extends TypePersistence {
    @Lob
    private byte[] value;
}
