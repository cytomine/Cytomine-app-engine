package be.cytomine.appengine.handlers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class StorageDataEntry {

    private byte[] data;
    private String name;
    private StorageDataEntry parent;
    private List<StorageDataEntry> children;
    private String storageId;
    private StorageDataType storageDataType;

    public StorageDataEntry(byte[] data, String name, String storageId, StorageDataType storageDataType) {
        this.data = data;
        this.name = name;
        this.children = new ArrayList<>();
        this.storageId = storageId;
        this.storageDataType = storageDataType;
    }

    public void addChild(StorageDataEntry child) {
        child.parent = this; // set the parent of the child
        this.children.add(child);
    }

    public String getAbsoluteStorageId() {
        StringBuilder absoluteStorageId = new StringBuilder(storageId);
        StorageDataEntry current = parent;
        while (Objects.nonNull(current)) {
            absoluteStorageId.insert(0, current.getStorageId() + "/");
            current = current.parent;
        }
        return absoluteStorageId.toString();
    }

    public StorageDataEntry(byte[] data) {
        this.data = data;
    }

    public StorageDataEntry(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public StorageDataEntry(String name, String storageId) {
        this.name = name;
        this.storageId = storageId;
    }
}
