package com.cloudant.sync.attachments;

import com.cloudant.sync.datastore.DocumentRevision;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomblench on 24/02/2014.
 */
public class MultipartWriter extends InputStream {

    MultipartWriter(DocumentRevision body, List<Attachment> attachments) {
        // TODO pick a boundary
        this.boundary = "--abc123".getBytes();
        this.trailingBoundary = "--abc123--".getBytes();
        components = new ArrayList<InputStream>();
        components.add(new ByteArrayInputStream(boundary));
        components.add(new ByteArrayInputStream(crlf));
        components.add(new ByteArrayInputStream(body.asBytes()));
        for (Attachment a : attachments) {
            components.add(new ByteArrayInputStream(crlf));
            components.add(new ByteArrayInputStream(boundary));
            components.add(new ByteArrayInputStream(crlf));
            components.add(a.data);
        }
        components.add(new ByteArrayInputStream(crlf));
        components.add(new ByteArrayInputStream(trailingBoundary));
        components.add(new ByteArrayInputStream(crlf));
        currentComponentIdx = 0;
    }

    private byte boundary[];
    private byte trailingBoundary[];
    private byte crlf[] = "\r\n".getBytes();
    private int currentComponentIdx;

    private ArrayList<InputStream> components;

    public int read() {
        // TODO call read with 1 byte
        return 0;
    }

    @Override
    public int read(byte[] bytes) throws java.io.IOException {
        int amountRead = 0;
        int currentOffset = 0;
        int howMuch = 0;
        do {
            InputStream currentComponent = components.get(currentComponentIdx);
            if (currentComponent.available() == 0) {
                ++currentComponentIdx;
                continue;
            }
            howMuch = Math.min(bytes.length - currentOffset, currentComponent.available());
            amountRead += currentComponent.read(bytes, currentOffset, howMuch);
            currentOffset += howMuch;
        } while (currentComponentIdx < components.size()-1 && howMuch > 0);
        return amountRead;
    }


}
