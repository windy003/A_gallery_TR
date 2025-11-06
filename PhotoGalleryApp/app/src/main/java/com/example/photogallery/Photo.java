package com.example.photogallery;

import java.io.Serializable;

public class Photo implements Serializable {
    private String path;
    private String name;
    private long dateAdded;
    private long size;

    public Photo(String path, String name, long dateAdded, long size) {
        this.path = path;
        this.name = name;
        this.dateAdded = dateAdded;
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public long getSize() {
        return size;
    }
}
