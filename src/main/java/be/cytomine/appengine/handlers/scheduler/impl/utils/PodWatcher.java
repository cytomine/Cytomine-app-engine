package be.cytomine.appengine.handlers.scheduler.impl.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.repositories.RunRepository;
import be.cytomine.appengine.states.TaskRunState;

@Slf4j
@AllArgsConstructor
@Component
public class PodWatcher implements Watcher<Pod> {

    private static final Map<String, TaskRunState> STATUS = new HashMap<String, TaskRunState>() {{
        put("Pending", TaskRunState.PENDING);
        put("Running", TaskRunState.RUNNING);
        put("Succeeded", TaskRunState.RUNNING);
        put("Failed", TaskRunState.FAILED);
        put("Unknown", TaskRunState.FAILED);
    }};

    private static final Set<TaskRunState> FINAL_STATES = Set.of(TaskRunState.FAILED, TaskRunState.FINISHED);

    private RunRepository runRepository;

    @Override
    public void eventReceived(Action action, Pod pod) {
        Map<String, String> labels = pod.getMetadata().getLabels();

        String runId = labels.get("runId");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            log.error("Pod Watcher: run {} is empty", runId);
            return;
        }

        Run run = runOptional.get();
        if (FINAL_STATES.contains(run.getState())) {
            return;
        }

        log.info("Pod Watcher: pod " + pod.getMetadata().getName());
        switch (action.name()) {
            case "ADDED":
                run.setState(TaskRunState.QUEUED);
                break;
            case "MODIFIED":
                run.setState(STATUS.getOrDefault(pod.getStatus().getPhase(), TaskRunState.FAILED));
                break;
            default:
                log.info("Unrecognized event: " + action.name());
        }

        run = runRepository.saveAndFlush(run);
        log.info("Pod Watcher: updated Run state to " + run.getState());
    }

    @Override
    public void onClose(WatcherException cause) {
        log.info("Watcher closed: " + cause.getMessage());
    }
}
