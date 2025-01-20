package be.cytomine.appengine.handlers;

import lombok.Data;

import java.util.LinkedList;
import java.util.Queue;

@Data
public class StorageData {
    StorageDataNode root;

    public StorageData(StorageDataNode root) {
        this.root = root;
    }
}
