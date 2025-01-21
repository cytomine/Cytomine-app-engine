package be.cytomine.appengine.services;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.dto.handlers.scheduler.Schedule;
import be.cytomine.appengine.dto.inputs.task.*;
import be.cytomine.appengine.dto.responses.errors.AppEngineError;
import be.cytomine.appengine.dto.responses.errors.ErrorBuilder;
import be.cytomine.appengine.dto.responses.errors.ErrorCode;
import be.cytomine.appengine.dto.responses.errors.details.ParameterError;
import be.cytomine.appengine.exceptions.*;
import be.cytomine.appengine.handlers.*;
import be.cytomine.appengine.models.task.*;
import be.cytomine.appengine.models.task.ParameterType;
import be.cytomine.appengine.repositories.*;
import be.cytomine.appengine.states.TaskRunState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class TaskProvisioningService {
    Logger logger = LoggerFactory.getLogger(TaskProvisioningService.class);

    private final TypePersistenceRepository typePersistenceRepository;
    private final RunRepository runRepository;
    private final FileStorageHandler fileStorageHandler;

    private SchedulerHandler schedulerHandler;
    @Value("${storage.input.charset}")
    private String charset;


    public TaskProvisioningService(SchedulerHandler schedulerHandler, RunRepository runRepository, FileStorageHandler fileStorageHandler, TypePersistenceRepository typePersistenceRepository) {
        this.runRepository = runRepository;
        this.fileStorageHandler = fileStorageHandler;
        this.typePersistenceRepository = typePersistenceRepository;
        this.schedulerHandler = schedulerHandler;
    }

    public JsonNode provisionRunParameter(JsonNode provision, String runId) throws ProvisioningException {
        logger.info("ProvisionParameter : finding associated task run...");
        Run run = getRunIfValid(runId);
        logger.info("ProvisionParameter : found");
        logger.info("ProvisionParameter : validating provision against parameter type definition...");
        GenericParameterProvision genericParameterProvision = new GenericParameterProvision();
        try {
            ObjectMapper mapper = new ObjectMapper();
            genericParameterProvision = mapper.treeToValue(provision, GenericParameterProvision.class);
            genericParameterProvision.setRunId(runId);

            validateProvisionValuesAgainstTaskType(genericParameterProvision, run);
        } catch (TypeValidationException e) {
            ParameterError parameterError = new ParameterError(provision.get("param_name").asText());
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST, parameterError);
            throw new ProvisioningException(error);
        } catch (JsonProcessingException e) {
            logger.info("ProvisionParameter : provision is not valid");
            ParameterError parameterError = new ParameterError(genericParameterProvision.getParameterName());
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_JSON_PROCESSING_ERROR, parameterError);
            throw new ProvisioningException(error);
        }
        logger.info("ProvisionParameter : provision is valid");
        logger.info("ProvisionParameter : storing provision to storage...");
        saveProvisionInStorage(provision, run);
        logger.info("ProvisionParameter : stored");
        logger.info("ProvisionParameter : saving provision in database...");
        saveInDatabase(provision, run);
        logger.info("ProvisionParameter : saved");

        if (run.getTask().getInputs().size() == 1) {
            changeStateToProvisioned(run);
        }

        return getInputParameterType(provision, run).createTypedParameterResponse(provision, run);
    }

    public JsonNode provisionRunParameter(String parameterName, String runId, byte[] value) throws ProvisioningException {
        logger.info("ProvisionParameter : finding associated task run...");
        Run run = getRunIfValid(runId);
        logger.info("ProvisionParameter : found");

        logger.info("ProvisionParameter : validating provision against parameter type definition...");
        GenericParameterProvision genericParameterProvision = new GenericParameterProvision();
        genericParameterProvision.setParameterName(parameterName);
        genericParameterProvision.setValue(value);
        genericParameterProvision.setRunId(runId);

        try {
            validateProvisionValuesAgainstTaskType(genericParameterProvision, run);
        } catch (TypeValidationException e) {
            ParameterError parameterError = new ParameterError(parameterName);
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_PARAMETER_DOES_NOT_EXIST, parameterError);
            throw new ProvisioningException(error);
        }
        logger.info("ProvisionParameter : provision is valid");

        logger.info("ProvisionParameter : storing provision to storage...");
        saveProvisionInStorage(parameterName, value, run);
        logger.info("ProvisionParameter : stored");

        logger.info("ProvisionParameter : saving provision in database...");
        saveInDatabase(parameterName, value, run);
        logger.info("ProvisionParameter : saved");

        if (run.getTask().getInputs().size() == 1) {
            changeStateToProvisioned(run);
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provision = mapper.createObjectNode();
        provision.put("param_name", parameterName);

        return getInputParameterType(parameterName, run).createTypedParameterResponse(provision, run);
    }

    private void changeStateToProvisioned(Run run) {
        logger.info("ProvisionParameter : Changing run state to PROVISIONED...");
        run.setState(TaskRunState.PROVISIONED);
        runRepository.saveAndFlush(run);
        logger.info("ProvisionParameter : RUN PROVISIONED");
    }

    public List<JsonNode> provisionMultipleRunParameters(String runId, List<JsonNode> provisions) throws ProvisioningException {
        List<JsonNode> response = new ArrayList<>();
        logger.info("ProvisionMultipleParameter : finding associated task run...");
        Run run = getRunIfValid(runId);
        logger.info("ProvisionMultipleParameter : found");
        logger.info("ProvisionMultipleParameter : handling provision list");
        // prepare error list just in case
        List<AppEngineError> multipleErrors = new ArrayList<>();
        for (JsonNode provision : provisions) {
            GenericParameterProvision genericParameterProvision = new GenericParameterProvision();
            try {
                ObjectMapper mapper = new ObjectMapper();
                genericParameterProvision = mapper.treeToValue(provision, GenericParameterProvision.class);
                genericParameterProvision.setRunId(runId);
                logger.info("ProvisionMultipleParameter : validating provision against parameter type definition...");

                validateProvisionValuesAgainstTaskType(genericParameterProvision, run);
            } catch (TypeValidationException e) {
                logger.info("ProvisionMultipleParameter : provision is invalid value validation failed");
                ParameterError parameterError = new ParameterError(genericParameterProvision.getParameterName());
                AppEngineError error = ErrorBuilder.build(e.getErrorCode(), parameterError);
                multipleErrors.add(error);
                continue;
            } catch (JsonProcessingException e) {
                logger.info("ProvisionMultipleParameter : provision is not invalid json processing failed");
                ParameterError parameterError = new ParameterError(genericParameterProvision.getParameterName());
                AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_JSON_PROCESSING_ERROR, parameterError);
                multipleErrors.add(error);
                continue;
            }
            logger.info("ProvisionMultipleParameter : provision is valid");
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }

        for (JsonNode provision : provisions) {
            logger.info("ProvisionMultipleParameter : storing provision to storage...");
            try {
                saveProvisionInStorage(provision, run);
            } catch (ProvisioningException e) {
                multipleErrors.add(e.getError());
                continue;
            }
            logger.info("ProvisionMultipleParameter : stored");
            logger.info("ProvisionMultipleParameter : saving provision in database...");
            saveInDatabase(provision, run);
            logger.info("ProvisionMultipleParameter : saved");
            JsonNode responseItem = getInputParameterType(provision, run).createTypedParameterResponse(provision, run);
            response.add(responseItem);
        }
        if (!multipleErrors.isEmpty()) {
            AppEngineError error = ErrorBuilder.buildBatchError(multipleErrors);
            throw new ProvisioningException(error);
        }
        if (provisions.size() == run.getTask().getInputs().size()) {
            changeStateToProvisioned(run);
        }
        logger.info("ProvisionMultipleParameter : return collection");
        return response;
    }

    @NotNull
    private void saveInDatabase(JsonNode provision, Run run) {
        Set<Input> inputs = run.getTask().getInputs();
        Input inputForType = inputs.stream().filter(input -> input.getName().equalsIgnoreCase(provision.get("param_name").asText())).findFirst().get();
        inputForType.getType().persistProvision(provision, run.getId());
    }

    @NotNull
    private void saveInDatabase(String parameterName, byte[] value, Run run) {
        Set<Input> inputs = run.getTask().getInputs();
        Input inputForType = inputs
            .stream()
            .filter(input -> input.getName().equalsIgnoreCase(parameterName))
            .findFirst()
            .get();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provision = mapper.createObjectNode();
        provision.put("param_name", parameterName);
        provision.put("value", value);

        inputForType
            .getType()
            .persistProvision(provision, run.getId());
    }

    private Type getInputParameterType(JsonNode provision, Run run) {
        Set<Input> inputs = run.getTask().getInputs();
        Input inputForType = inputs.stream().filter(input -> input.getName().equalsIgnoreCase(provision.get("param_name").asText())).findFirst().get();
        return inputForType.getType();
    }

    private Type getInputParameterType(String parameterName, Run run) {
        Set<Input> inputs = run.getTask().getInputs();
        Input inputForType = inputs
            .stream()
            .filter(input -> input.getName().equalsIgnoreCase(parameterName))
            .findFirst()
            .get();
        return inputForType.getType();
    }

    private void saveProvisionInStorage(JsonNode provision, Run run) throws ProvisioningException {
        Set<Input> inputs = run.getTask().getInputs();
        Input inputForType = inputs.stream().filter(input -> input.getName().equalsIgnoreCase(provision.get("param_name").asText())).findFirst().get();

        Storage runStorage = new Storage("task-run-inputs-" + run.getId());
        // Todo : make this return StorageData (DONE)
        StorageData inputProvisionFileData = inputForType.getType().mapToStorageFileData(provision,charset);
//        FileData inputProvisionFileData = inputForType.getType().mapToStorageFileData(provision, charset);
        try {
            // Todo : use saveToStorage() instead of genereic createFile() (DONE)
            fileStorageHandler.saveToStorage(runStorage, inputProvisionFileData);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(ErrorCode.STORAGE_STORING_INPUT_FAILED, provision.get("param_name").asText(), e.getMessage());
            throw new ProvisioningException(error);

        }
    }

    private void saveProvisionInStorage(String parameterName, byte[] value, Run run) throws ProvisioningException {
        Set<Input> inputs = run.getTask().getInputs();
        Input inputForType = inputs.stream().filter(input -> input.getName().equalsIgnoreCase(parameterName)).findFirst().get();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode provision = mapper.createObjectNode();
        provision.put("param_name", parameterName);
        provision.put("value", value);

        Storage runStorage = new Storage("task-run-inputs-" + run.getId());
        // Todo : make this return StorageData (DONE)
        StorageData inputProvisionFileData = inputForType.getType().mapToStorageFileData(provision,charset);
//        FileData inputProvisionFileData = inputForType.getType().mapToStorageFileData(provision, charset);
        try {
            // Todo : use saveToStorage() instead of genereic createFile() (DONE)
            fileStorageHandler.saveToStorage(runStorage, inputProvisionFileData);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(ErrorCode.STORAGE_STORING_INPUT_FAILED, parameterName, e.getMessage());
            throw new ProvisioningException(error);
        }
    }

    private static void validateProvisionValuesAgainstTaskType(GenericParameterProvision provision, Run run) throws TypeValidationException {
        Task task = run.getTask();
        Set<Input> inputs = task.getInputs();
        boolean inputFound = false;
        for (Input input : inputs) {
            if (input.getName().equalsIgnoreCase(provision.getParameterName())) {
                inputFound = true;
                input.getType().validate(provision.getValue());
            }
        }
        if (!inputFound) {
            throw new TypeValidationException("unknown parameter [" + provision.getParameterName() + "], not found in task descriptor");
        }
    }

    public FileData retrieveInputsZipArchive(String runId) throws ProvisioningException, FileStorageException, IOException {
        logger.info("Retrieving Inputs Archive : retrieving...");
        Run run = getRunIfValid(runId);
        if (run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        logger.info("Retrieving Inputs Archive : fetching from storage...");
        List<TypePersistence> provisions = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), ParameterType.INPUT);
        if (provisions.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_PROVISIONS_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logger.info("Retrieving Inputs Archive : zipping...");
        ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream);
        for (TypePersistence provision : provisions) {
            // Todo : readFile() also is designed with parameter -> file assumption .. poll StorageData to build the Zip archive [DONE]
            StorageData provisionFileData = fileStorageHandler.readFile(new StorageData(provision.getParameterName(), "task-run-inputs-" + run.getId()));
            while (!provisionFileData.isEmpty()){
                StorageDataEntry current = provisionFileData.poll();
                String name = null;
                if(current.getStorageDataType().equals(StorageDataType.FILE)){
                    // it's a file matching a provision
                    name = current.getName();
                }
                if(current.getStorageDataType().equals(StorageDataType.DIRECTORY)){
                    // it's a directory matching a provision .. add with a trailing /
                    name = current.getName() + "/";
                }
                ZipEntry zipEntry = new ZipEntry(current.getName());
                zipOut.putNextEntry(zipEntry);
                if(current.getStorageDataType().equals(StorageDataType.FILE)){
                    zipOut.write(current.getData());
                }
                zipOut.closeEntry();
            }
        }
        zipOut.close();
        byteArrayOutputStream.close();
        logger.info("Retrieving Inputs Archive : zipped...");
        return new FileData(byteArrayOutputStream.toByteArray());
    }

    public FileData retrieveOutputsZipArchive(String runId) throws ProvisioningException, FileStorageException, IOException {
        logger.info("Retrieving Outputs Archive : retrieving...");
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }

        Run run = runOptional.get();
        if (!run.getState().equals(TaskRunState.FINISHED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        // fetch results from storage
        List<TypePersistence> results = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(runOptional.get().getId(), ParameterType.OUTPUT);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        logger.info("Retrieving Outputs Archive : zipping...");
        ZipOutputStream zipOut = new ZipOutputStream(byteArrayOutputStream);
        for (TypePersistence result : results) {
            // Todo : readFile() also is designed with parameter -> file assumption .. poll to build Zip archive [DONE]
            StorageData provisionFileData = fileStorageHandler.readFile(new StorageData(result.getParameterName(), "task-run-outputs-" + run.getId()));
            while (!provisionFileData.isEmpty()){
                StorageDataEntry current = provisionFileData.poll();
                String name = null;
                if(current.getStorageDataType().equals(StorageDataType.FILE)){
                    // it's a file matching a provision
                    name = current.getName();
                }
                if(current.getStorageDataType().equals(StorageDataType.DIRECTORY)){
                    // it's a directory matching a provision .. add with a trailing /
                    name = current.getName() + "/";
                }
                ZipEntry zipEntry = new ZipEntry(current.getName());
                zipOut.putNextEntry(zipEntry);
                if(current.getStorageDataType().equals(StorageDataType.FILE)){
                    zipOut.write(current.getData());
                }
                zipOut.closeEntry();
            }
        }
        zipOut.close();
        byteArrayOutputStream.close();
        logger.info("Retrieving Outputs Archive : zipped...");
        return new FileData(byteArrayOutputStream.toByteArray());
    }

    public List<TaskRunParameterValue> postOutputsZipArchive(String runId, MultipartFile outputs) throws ProvisioningException {
        logger.info("Posting Outputs Archive : posting...");
        Run run = getRunIfValid(runId);
        if (notInOneOfSchedulerManagedStates(run)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        Set<Output> runTaskOutputs = run.getTask().getOutputs();
        new ArrayList<>();
        logger.info("Posting Outputs Archive : unzipping...");
        try {
            List<TaskRunParameterValue> outputList = processOutputFiles(outputs, runTaskOutputs, run);
            run.setState(TaskRunState.FINISHED);
            runRepository.saveAndFlush(run);
            logger.info("Posting Outputs Archive : updated Run state to FINISHED");
            return outputList;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private List<TaskRunParameterValue> processOutputFiles(MultipartFile outputs, Set<Output> runTaskOutputs, Run run) throws IOException, ProvisioningException {
        // read files from the archive
        try (ZipArchiveInputStream multiPartFileZipInputStream = new ZipArchiveInputStream(outputs.getInputStream())) {
            logger.info("Posting Outputs Archive : unzipped");
            List<Output> remainingOutputs = new ArrayList<>(runTaskOutputs);
            List<TaskRunParameterValue> taskRunParameterValues = new ArrayList<>();
            // Todo : here it loops to read files in the archive assuming all parameters are files in the archive [DONE]
            // Todo : make sure directories are checked against types and properly processed [DONE]
            ZipEntry ze;
            while ((ze = multiPartFileZipInputStream.getNextZipEntry()) != null) {
                // look for output matching file name
                boolean fileForComplexType = false;
                Output currentOutput = null;
                for (int i = 0; i < remainingOutputs.size(); i++) {
                    currentOutput = remainingOutputs.get(i);
                    if (currentOutput.getName().equals(ze.getName())) { // assuming it's a file
                        remainingOutputs.remove(i);
                        break;
                    }
                    // remove the trailing slash
                    String noTrailingSlash = ze.getName().replace("/" , "");
                    if (currentOutput.getName().equals(noTrailingSlash)) { // assuming it's a directory
                        currentOutput.setName(ze.getName());
                        remainingOutputs.remove(i);
                        fileForComplexType = true;
                        break;
                    }
                    currentOutput = null;
                }

                // there's a file that do not match any output parameter
                if (currentOutput == null) {
                    AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_UNKNOWN_OUTPUT);
                    logger.info("Posting Outputs Archive : output invalid (unknown output)");
                    run.setState(TaskRunState.FAILED);
                    runRepository.saveAndFlush(run);
                    logger.info("Posting Outputs Archive : updated Run state to FAILED");
                    throw new ProvisioningException(error);
                }

                // Todo : reading a single file which does not work with complex types
                // Todo : make sure if a directory exists it matches a type definition and properly mapped to StorageData
                // read file
                String outputName = currentOutput.getName();
                // reads content into byte[] if the ZipEntry is a file
                // if directory we read all the zip entries starting with the name of the parameter
                byte[] rawOutput = multiPartFileZipInputStream.readNBytes((int) ze.getSize());
                String output = new String(rawOutput, getStorageCharset(charset));

                String trimmedOutput = output.trim();
                // saving to database does not care about the type
                saveOutput(run, currentOutput, trimmedOutput);
                // saving to the storage does not care about the type
                // Todo : should not get raw data byte array .. it should rather get StorageData
                storeOutputInFileStorage(run, rawOutput, outputName);
                // based on parsed type build the response
                taskRunParameterValues.add(currentOutput.getType().buildTaskRunParameterValue(trimmedOutput, run.getId(), outputName));
            }

            if (!remainingOutputs.isEmpty()) {
                AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_MISSING_OUTPUTS);
                logger.info("Posting Outputs Archive : output invalid (missing outputs)");
                run.setState(TaskRunState.FAILED);
                runRepository.saveAndFlush(run);
                logger.info("Posting Outputs Archive : updated Run state to FAILED");
                throw new ProvisioningException(error);
            }

            logger.info("Posting Outputs Archive : posted");
            return taskRunParameterValues;
        }
    }

    public Charset getStorageCharset(String charset) {
        return switch (charset.toUpperCase()) {
            case "US_ASCII" -> StandardCharsets.US_ASCII;
            case "ISO_8859_1" -> StandardCharsets.ISO_8859_1;
            case "UTF_16LE" -> StandardCharsets.UTF_16LE;
            case "UTF_16BE" -> StandardCharsets.UTF_16BE;
            case "UTF_16" -> StandardCharsets.UTF_16;
            default -> StandardCharsets.UTF_8;
        };
    }

    private void storeOutputInFileStorage(Run run, byte[] outputValue, String name) throws ProvisioningException {
        logger.info("Posting Outputs Archive : storing in file storage...");
        Storage outputsStorage = new Storage("task-run-outputs-" + run.getId());
        // Todo : create a new StorageData [DONE]
        StorageData outputFileData = new StorageData(outputValue, name);
        try {
            // Todo : use saveToStorage() instead of genereic createFile() [DONE]
            fileStorageHandler.saveToStorage(outputsStorage, outputFileData);
        } catch (FileStorageException e) {
            run.setState(TaskRunState.FAILED);
            runRepository.saveAndFlush(run);
            logger.info("Posting Outputs Archive : updated Run state to FAILED");
            AppEngineError error = ErrorBuilder.buildParamRelatedError(ErrorCode.STORAGE_STORING_INPUT_FAILED, name, e.getMessage());
            throw new ProvisioningException(error);
        }
        logger.info("Posting Outputs Archive : stored");
    }

    private void saveOutput(Run run, Output currentOutput, String outputValue) {
        logger.info("Posting Outputs Archive : saving...");
        currentOutput.getType().persistResult(run, currentOutput, outputValue);
        logger.info("Posting Outputs Archive : saved...");
    }

    private static boolean notInOneOfSchedulerManagedStates(Run run) {
        return !run.getState().equals(TaskRunState.RUNNING) && !run.getState().equals(TaskRunState.PENDING) && !run.getState().equals(TaskRunState.QUEUED) && !run.getState().equals(TaskRunState.QUEUING);
    }

    public List<TaskRunParameterValue> retrieveRunOutputs(String runId) throws ProvisioningException {
        logger.info("Retrieving Outputs Json : retrieving...");
        // validate run
        Run run = getRunIfValid(runId);
        if (!run.getState().equals(TaskRunState.FINISHED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // find all the results
        List<TaskRunParameterValue> outputList = buildTaskRunParameterValues(run, ParameterType.OUTPUT);
        logger.info("Retrieving Outputs Json : retrieved");
        return outputList;
    }

    private Run getRunIfValid(String runId) throws ProvisioningException {
        Optional<Run> runOptional = runRepository.findById(UUID.fromString(runId));
        if (runOptional.isEmpty()) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.RUN_NOT_FOUND);
            throw new ProvisioningException(error);
        }
        // check the state is valid
        return runOptional.get();
    }

    public List<TaskRunParameterValue> retrieveRunInputs(String runId) throws ProvisioningException {
        logger.info("Retrieving Inputs : retrieving...");
        // validate run
        Run run = getRunIfValid(runId);
        if (run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }
        // find all the results
        List<TaskRunParameterValue> inputList = buildTaskRunParameterValues(run, ParameterType.INPUT);
        logger.info("Retrieving Inputs : retrieved");
        return inputList;
    }

    public byte[] retrieveSingleRunIO(String runId, String parameterName, ParameterType type) throws ProvisioningException {
        logger.info("Get IO file from storage: searching...");

        String io = type.equals(ParameterType.INPUT) ? "inputs" : "outputs";
        Storage storage = new Storage("task-run-" + io + "-" + runId);
        // Todo : use StorageData instead of FileData [DONE]
        StorageData data = new StorageData(parameterName, storage.getIdStorage());

        logger.info("Get IO file from storage: read file " + parameterName + " from storage...");
        try {
            data = fileStorageHandler.readFile(data);
        } catch (FileStorageException e) {
            AppEngineError error = ErrorBuilder.buildParamRelatedError(
                ErrorCode.STORAGE_READING_FILE_FAILED,
                parameterName,
                e.getMessage()
            );
            throw new ProvisioningException(error);
        }

        logger.info("Get IO file from storage: done");
        return data.peek().getData();
    }

    private List<TaskRunParameterValue> buildTaskRunParameterValues(Run run, ParameterType type) {
        List<TaskRunParameterValue> parameterValues = new ArrayList<>();
        List<TypePersistence> results = typePersistenceRepository.findTypePersistenceByRunIdAndParameterType(run.getId(), type);
        if (type.equals(ParameterType.INPUT)) {
            Set<Input> inputs = run.getTask().getInputs();
            for (TypePersistence result : results) {
                // based on the type of the parameter assign the type
                Input inputForType = inputs.stream().filter(input -> input.getName().equalsIgnoreCase(result.getParameterName())).findFirst().get();
                parameterValues.add(inputForType.getType().buildTaskRunParameterValue(result));
            }
        } else {
            Set<Output> outputs = run.getTask().getOutputs();
            for (TypePersistence result : results) {
                // based on the type of the parameter assign the type
                Output outputForType = outputs.stream().filter(output -> output.getName().equalsIgnoreCase(result.getParameterName())).findFirst().get();
                parameterValues.add(outputForType.getType().buildTaskRunParameterValue(result));
            }
        }

        return parameterValues;
    }

    public StateAction updateRunState(String runId, State state) throws SchedulingException, ProvisioningException {
        logger.info("Update State: validating Run...");
        Run run = getRunIfValid(runId);

        return switch (state.getDesired()) {
            case PROVISIONED -> updateToProvisioned(run);
            case RUNNING -> run(run);
            // to safeguard against unknown state transition requests
            default -> throw new ProvisioningException(ErrorBuilder.build(ErrorCode.UKNOWN_STATE));
        };
    }

    private StateAction createStateAction(Run run, TaskRunState state) {
        StateAction action = new StateAction();
        action.setStatus("success");

        TaskDescription description = makeTaskDescription(run.getTask());
        Resource resource = new Resource(description, run.getId(), state, new Date(), new Date(), new Date());
        action.setResource(resource);

        return action;
    }

    @NotNull
    private StateAction run(Run run) throws ProvisioningException, SchedulingException {
        logger.info("Running Task : scheduling...");
        if (!run.getState().equals(TaskRunState.PROVISIONED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_NOT_PROVISIONED);
            throw new ProvisioningException(error);
        }
        logger.info("Running Task : valid run");

        logger.info("Running Task : contacting scheduler...");
        Schedule schedule = new Schedule();
        schedule.setRun(run);
        schedulerHandler.schedule(schedule);
        logger.info("Running Task : scheduling done");

        // update the final state
        run.setState(TaskRunState.QUEUING);
        runRepository.saveAndFlush(run);
        logger.info("Running Task : updated Run state to QUEUING");

        StateAction action = createStateAction(run, TaskRunState.QUEUING);
        logger.info("Running Task : scheduled");

        return action;
    }

    private StateAction updateToProvisioned(Run run) throws ProvisioningException {
        logger.info("Provisioning: update state to PROVISIONED...");
        if (!run.getState().equals(TaskRunState.CREATED)) {
            AppEngineError error = ErrorBuilder.build(ErrorCode.INTERNAL_INVALID_TASK_RUN_STATE);
            throw new ProvisioningException(error);
        }

        changeStateToProvisioned(run);

        logger.info("Provisioning: state updated to PROVISIONED");

        return createStateAction(run, TaskRunState.PROVISIONED);
    }

    public TaskDescription makeTaskDescription(Task task) {
        TaskDescription taskDescription = new TaskDescription(task.getIdentifier(), task.getName(), task.getNamespace(), task.getVersion(), task.getDescription());
        Set<TaskAuthor> descriptionAuthors = new HashSet<>();
        for (Author author : task.getAuthors()) {
            TaskAuthor taskAuthor = new TaskAuthor(author.getFirstName(), author.getLastName(), author.getOrganization(), author.getEmail(), author.isContact());
            descriptionAuthors.add(taskAuthor);
        }
        taskDescription.setAuthors(descriptionAuthors);
        return taskDescription;
    }

    public TaskRunResponse retrieveRun(String runId) throws ProvisioningException {
        logger.info("Retrieving Run : retrieving...");
        Run run = getRunIfValid(runId);
        TaskDescription description = makeTaskDescription(run.getTask());
        logger.info("Retrieving Run : retrieved");
        return new TaskRunResponse(description, UUID.fromString(runId), run.getState(), run.getCreated_at(), run.getUpdated_at(), run.getLast_state_transition_at());
    }
}
