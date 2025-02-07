package be.cytomine.appengine.dto.inputs.task;

import java.util.Date;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

import be.cytomine.appengine.states.TaskRunState;

@Data
@AllArgsConstructor
public class Resource {
    private UUID id;

    private TaskDescription task;

    private TaskRunState state;

    private Date createdAt;

    private Date updatedAt;

    private Date lastStateTransitionAt;
}
