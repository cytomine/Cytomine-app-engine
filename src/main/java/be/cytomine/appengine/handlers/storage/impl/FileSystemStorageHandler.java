package be.cytomine.appengine.handlers.storage.impl;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.FileStorageException;
import be.cytomine.appengine.handlers.*;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Stream;

public class FileSystemStorageHandler implements FileStorageHandler {

    @Value("${storage.base-path}")
    private String basePath;

    // Level-order traversal with levels separated
    public void saveToStorage(Storage storage , StorageData storageData) throws FileStorageException {
        if (storageData.peek() == null) return;
        while (!storageData.isEmpty()) {
                StorageDataEntry current = storageData.poll();
            String filename = current.getName();
            String storageId = storage.getIdStorage();
                if (current == null) continue;
                // process the node here
                if(current.getStorageDataType() == StorageDataType.FILE){
                    // Todo : create path by traversing all the way up to root (DONE)
                    try {
                        Path filePath = Paths.get(basePath, storageId, filename);
                        Files.write(filePath, current.getData());
                    } catch (IOException e) {
                        String error = "Failed to create file " + filename;
                        error += " in storage " + storageId + ": " + e.getMessage();
                        throw new FileStorageException(error);
                    }
                }

                if(current.getStorageDataType() == StorageDataType.DIRECTORY){
                    // Todo : create path by traversing all the way up to root (DONE)
                    try {
                        Path path = Paths.get(basePath, storageId);
                        Files.createDirectories(path);
                    } catch (IOException e) {
                        String error = "Failed to create storage " + storageId + ": " + e.getMessage();
                        throw new FileStorageException(error);
                    }
                    
                }

        }
    }

    @Override
    public void createStorage(Storage storage) throws FileStorageException {
        String storageId = storage.getIdStorage();

        try {
            Path path = Paths.get(basePath, storageId);
            Files.createDirectories(path);
        } catch (IOException e) {
            String error = "Failed to create storage " + storageId + ": " + e.getMessage();
            throw new FileStorageException(error);
        }
    }

    @Override
    public void deleteStorage(Storage storage) throws FileStorageException {
        String storageId = storage.getIdStorage();

        try {
            Path path = Paths.get(basePath, storageId);
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            String error = "Failed to delete storage " + storageId + ": " + e.getMessage();
            throw new FileStorageException(error);
        }
    }

    @Override
    public void createFile(Storage storage, FileData file) throws FileStorageException {
        // Todo : this is replaced by saveToStorage() for provisioning related operations (DONE)
        String filename = file.getFileName();
        String storageId = storage.getIdStorage();

        try {
            Path filePath = Paths.get(basePath, storageId, filename);
            Files.write(filePath, file.getFileData());
        } catch (IOException e) {
            String error = "Failed to create file " + filename;
            error += " in storage " + storageId + ": " + e.getMessage();
            throw new FileStorageException(error);
        }
    }

    @Override
    public boolean checkStorageExists(Storage storage) throws FileStorageException {
        return Files.exists(Paths.get(basePath, storage.getIdStorage()));
    }

    @Override
    public boolean checkStorageExists(String idStorage) throws FileStorageException {
        return Files.exists(Paths.get(basePath, idStorage));
    }

    @Override
    public void deleteFile(StorageData file) throws FileStorageException {
        // Todo : delete the file or the root directory of a complex type
        String filename = file.peek().getName();

        try {
            Path filePath = Paths.get(basePath, file.peek().getStorageId(), filename);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new FileStorageException("Failed to delete file " + filename);
        }
    }

    @Override
    public StorageData readFile(StorageData emptyFile) throws FileStorageException {
        // Todo : read the parameter file or a complete directory when applicable
        StorageDataEntry current = emptyFile.poll();
        String filename = current.getName();
        Path filePath = Paths.get(basePath, current.getStorageId(), filename);
        try {
            Files.walk(filePath).forEach(path -> {
                if (Files.isRegularFile(path)) {
                    try {
                        current.setData(Files.readAllBytes(path));
                        current.setStorageDataType(StorageDataType.FILE);
                        emptyFile.add(current);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

                if (Files.isDirectory(path)) {
                    current.setStorageDataType(StorageDataType.DIRECTORY);
                    emptyFile.add(current);
                }
            });
            return emptyFile;
        } catch (IOException | RuntimeException e) {
            throw new FileStorageException("Failed to read file " + filename);
        }
    }
}
