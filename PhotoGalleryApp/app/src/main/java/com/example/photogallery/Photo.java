package com.example.photogallery;

import android.net.Uri;
import java.io.Serializable;

public class Photo implements Serializable {
    private long id;
    private String path;
    private String name;
    private long dateAdded;
    private long size;
    private transient Uri uri; // transient 因为 Uri 不能直接序列化

    public Photo(long id, String path, String name, long dateAdded, long size) {
        this.id = id;
        this.path = path;
        this.name = name;
        this.dateAdded = dateAdded;
        this.size = size;
    }

    public long getId() {
        return id;
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

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }
}
