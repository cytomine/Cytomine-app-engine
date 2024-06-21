package be.cytomine.appengine.models.task.image;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import be.cytomine.appengine.dto.inputs.task.types.image.ImageTypeConstraint;
import be.cytomine.appengine.dto.inputs.task.types.image.ImageValue;
import be.cytomine.appengine.handlers.FileData;
import be.cytomine.appengine.models.task.Output;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.models.task.Run;
import be.cytomine.appengine.models.task.Type;
import be.cytomine.appengine.models.task.TypePersistence;
import be.cytomine.appengine.models.task.ValueType;
import be.cytomine.appengine.repositories.image.ImagePersistenceRepository;
import be.cytomine.appengine.utils.AppEngineApplicationContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
public class ImageType extends Type {

    @Column(nullable = true)
    private Long maxFileSize;

    @Column(nullable = true)
    private Long maxWidth;

    @Column(nullable = true)
    private Long maxHeight;

    private List<String> formats;

    public void setConstraint(ImageTypeConstraint constraint, Long value) {
        switch (constraint) {
            case MAX_FILE_SIZE:
                this.setMaxFileSize(value);
                break;
            case MAX_WIDTH:
                this.setMaxWidth(value);
                break;
            case MAX_HEIGHT:
                this.setMaxHeight(value);
                break;
        }
    }

    @Override
    public void validate(Object valueObject) {

    }

    @Override
    public void persistProvision(JsonNode provision, UUID runId) {
        ImagePersistenceRepository imagePersistenceRepository = AppEngineApplicationContext.getBean(ImagePersistenceRepository.class);
        String parameterName = provision.get("param_name").asText();
        byte[] value = null;
        try {
            value = provision.get("value").binaryValue();
        } catch (IOException ignored) {}
        ImagePersistence persistedProvision = imagePersistenceRepository.findImagePersistenceByParameterNameAndRunIdAndParameterType(parameterName, runId, ParameterType.INPUT);
        if (persistedProvision == null) {
            persistedProvision = new ImagePersistence();
            persistedProvision.setParameterName(parameterName);
            persistedProvision.setParameterType(ParameterType.INPUT);
            persistedProvision.setRunId(runId);
            persistedProvision.setValue(value);
            persistedProvision.setValueType(ValueType.IMAGE);
            imagePersistenceRepository.save(persistedProvision);
        } else {
            persistedProvision.setValue(value);
            imagePersistenceRepository.saveAndFlush(persistedProvision);
        }
    }

    @Override
    public void persistResult(Run run, Output currentOutput, String outputValue) {
        ImagePersistenceRepository imagePersistenceRepository = AppEngineApplicationContext.getBean(ImagePersistenceRepository.class);
        ImagePersistence result = imagePersistenceRepository.findImagePersistenceByParameterNameAndRunIdAndParameterType(currentOutput.getName(), run.getId(), ParameterType.OUTPUT);
        if (result == null) {
            result = new ImagePersistence();
            result.setParameterType(ParameterType.OUTPUT);
            result.setParameterName(currentOutput.getName());
            result.setRunId(run.getId());
            result.setValueType(ValueType.IMAGE);

            try {
                result.setValue(outputValue.getBytes());
            } catch (Exception ignored) {}

            imagePersistenceRepository.save(result);
        }
    }

    @Override
    public FileData mapToStorageFileData(JsonNode provision, String charset) {
        String parameterName = provision.get("param_name").asText();
        byte[] inputFileData = null;
        try {
            inputFileData = provision.get("value").binaryValue();
        } catch (IOException ignored) {}
        return new FileData(inputFileData, parameterName);
    }

    @Override
    public JsonNode createTypedParameterResponse(JsonNode provision, Run run) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provisionedParameter = mapper.createObjectNode();
        provisionedParameter.put("param_name", provision.get("param_name").asText());
        provisionedParameter.put("value", provision.get("value").asInt());
        provisionedParameter.put("task_run_id", String.valueOf(run.getId()));
        return provisionedParameter;
    }

    @Override
    public ImageValue buildTaskRunParameterValue(String output, UUID id, String outputName) {
        ImageValue imageValue = new ImageValue();
        imageValue.setTask_run_id(id);
        imageValue.setValue(output.getBytes());
        imageValue.setParam_name(outputName);
        return imageValue;
    }

    @Override
    public ImageValue buildTaskRunParameterValue(TypePersistence typePersistence) {
        ImagePersistence imagePersistence = (ImagePersistence) typePersistence;
        ImageValue imageValue = new ImageValue();
        imageValue.setTask_run_id(imagePersistence.getRunId());
        imageValue.setValue(imagePersistence.getValue());
        imageValue.setParam_name(imagePersistence.getParameterName());
        return imageValue;
    }
}
