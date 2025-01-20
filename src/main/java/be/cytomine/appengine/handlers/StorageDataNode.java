package be.cytomine.appengine.handlers;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Data
public class StorageDataNode {

    private byte[] data;
    private String name;
    private StorageDataNode parent;
    private List<StorageDataNode> children;
    private String storageId;
    private StorageDataType storageDataType;

    public StorageDataNode(byte[] data, String name, String storageId, StorageDataType storageDataType) {
        this.data = data;
        this.name = name;
        this.children = new ArrayList<>();
        this.storageId = storageId;
        this.storageDataType = storageDataType;
    }

    public void addChild(StorageDataNode child) {
        child.parent = this; // set the parent of the child
        this.children.add(child);
    }

    public String getAbsoluteStorageId() {
        StringBuilder absoluteStorageId = new StringBuilder(storageId);
        StorageDataNode current = parent;
        while (Objects.nonNull(current)) {
            absoluteStorageId.insert(0, current.getStorageId() + "/");
            current = current.parent;
        }
        return absoluteStorageId.toString();
    }

    public StorageDataNode(byte[] data) {
        this.data = data;
    }

    public StorageDataNode(byte[] data, String name) {
        this.data = data;
        this.name = name;
    }

    public StorageDataNode(String name, String storageId) {
        this.name = name;
        this.storageId = storageId;
    }
}
