package com.cloudant.sync.datastore;

import java.io.InputStream;

/**
 * Created by tomblench on 24/02/2014.
 */
public class Attachment implements Comparable<Attachment>{

    public String name;
    public String contentType;
    public InputStream data;
    public long length;
    public byte[] md5;

    public int compareTo(Attachment other) {
        return name.compareTo(other.name);
    }

    
}
