package be.cytomine.appengine.handlers;

import be.cytomine.appengine.dto.handlers.filestorage.Storage;
import be.cytomine.appengine.exceptions.FileStorageException;

public interface FileStorageHandler {
    void createStorage(Storage storage)
        throws FileStorageException;

    void deleteStorage(Storage storage)
        throws FileStorageException;

    void createFile(Storage storage, FileData file)
        throws FileStorageException;

    boolean checkStorageExists(Storage storage)
        throws FileStorageException;

    boolean checkStorageExists(String idStorage)
        throws FileStorageException;

    void deleteFile(FileData file)
        throws FileStorageException;

    void saveToStorage(Storage storage , StorageData storageData) throws FileStorageException;

    FileData readFile(FileData emptyFile) throws FileStorageException;
}
