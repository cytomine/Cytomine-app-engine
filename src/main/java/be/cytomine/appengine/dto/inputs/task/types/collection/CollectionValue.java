package be.cytomine.appengine.dto.inputs.task.types.collection;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import be.cytomine.appengine.dto.inputs.task.TaskRunParameterValue;


@EqualsAndHashCode(callSuper = false)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CollectionValue extends TaskRunParameterValue {
    @JsonIgnoreProperties({"param_name"})
    private List<TaskRunParameterValue> value;


}
