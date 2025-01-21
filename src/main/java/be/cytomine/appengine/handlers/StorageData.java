package be.cytomine.appengine.handlers;

import lombok.Data;

import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;

@Data
public class StorageData {
    Queue<StorageDataEntry> queue;

    public StorageData(StorageDataEntry root) {
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageData(byte[] data, String name) {
        StorageDataEntry root = new StorageDataEntry(data);
        root.setName(name);
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageData(byte[] fileData) {
        StorageDataEntry root = new StorageDataEntry(fileData);
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageData(String parameterName, String s) {
        StorageDataEntry root = new StorageDataEntry(parameterName, s);
        queue = new LinkedList<>();
        queue.add(root);
    }

    public StorageDataEntry peek() {
        return queue.peek();
    }

    public boolean add(StorageDataEntry entry) {
        if(Objects.isNull(entry)) {
            return false; // null values are not allowed in StorageData
        }
        return queue.add(entry);
    }

    public StorageDataEntry poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }
}
