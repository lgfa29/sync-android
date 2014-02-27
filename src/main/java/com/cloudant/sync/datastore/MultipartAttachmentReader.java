package com.cloudant.sync.datastore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomblench on 24/02/2014.
 */
public class MultipartAttachmentReader extends OutputStream {

    private byte[] boundary;
    private int boundaryCount = 0;

    private class Matcher {
        private byte[] toMatch;
        private byte[] circularBuffer;
        private int off = 0;

        private byte[] potentialMatch;

        public Matcher(byte[] toMatch) {
            this.toMatch = toMatch;
            this.circularBuffer = new byte[toMatch.length];
        }

        // put the next byte into the circular buffer and see if we have a match yet
        public boolean match(byte c) {
            circularBuffer[off++] = c;
            off %= toMatch.length;
            for (int i=0; i<toMatch.length; i++) {
                if (toMatch[i] != circularBuffer[(i+off) % toMatch.length])
                    return false;
                }
            return true;
            }
        }

    private Matcher boundaryMatcher;
    private int state;
    private boolean inSection;
    byte[] currentSection; // body or attachment
    OutputStream currentStream;
    public List<OutputStream> streams;

    MultipartAttachmentReader(byte[] boundary) {
        this.boundary = boundary;
        state = 0;
        inSection = false;
        boundaryMatcher = new Matcher(boundary);
        currentStream = new ByteArrayOutputStream();
        streams = new ArrayList<OutputStream>();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException{
        List<Integer> boundaries;
        boundaries = new ArrayList<Integer>();
        for (int i=off; i<len-off; i++) {
            if (boundaryMatcher.match(b[i])) {
                boundaries.add(i+1);
                boundaryCount++;
                System.out.println("Matched at off " + i);
                // next four bytes - \r\n\r\n or --\r\n
            }
        }
        // any new boundaries?
        if (boundaries.size() > 0) {
            for (int i=0; i<boundaries.size(); i++) {
                if (i == 0) {
                    // first one, write into current stream up to this point
                    int bOff = boundaries.get(i);
                    currentStream.write(b, 0, bOff);
                }
                if (i == boundaries.size() -1) {
                    // last one, write to the end
                    int bOff = boundaries.get(i);
                    currentStream = new ByteArrayOutputStream();
                    streams.add(currentStream);
                    currentStream.write(b, bOff, len-bOff);
                }
                if (i != 0 && i != boundaries.size() -1) {
                    int bOff1 = boundaries.get(i);
                    int bOff2 = boundaries.get(i+1);
                    currentStream = new ByteArrayOutputStream();
                    streams.add(currentStream);
                    currentStream.write(b, bOff1, bOff2-bOff1);
                }
            }
        } else {
            // just write everything
            if (currentStream != null) {
                currentStream.write(b, off, len);
            }
        }

    }

    @Override
    public void write(byte[] b) throws IOException {
        this.write(b, 0, b.length);
    }

    public int getBoundaryCount() {
        return boundaryCount;
    }

    @Override
    public void write(int b) {
        // TODO
    }



}
