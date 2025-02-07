package be.cytomine.appengine.dto.inputs.task;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

import be.cytomine.appengine.states.TaskRunState;

@Data
@AllArgsConstructor
public class TaskRunResponse {
    private UUID id;

    private TaskDescription task;

    private TaskRunState state;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastStateTransitionAt;
}
