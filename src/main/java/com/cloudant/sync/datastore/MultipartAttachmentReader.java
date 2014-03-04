package com.cloudant.sync.datastore;

import com.cloudant.sync.util.JSONUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by tomblench on 24/02/2014.
 */
public class MultipartAttachmentReader extends OutputStream {

    private class Section {
        public Section() {
            try {
                md5 = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Cannot initialise MD5");
            }
        }
        public void write(byte[] input, int offset, int len) throws IOException {
            int curSkip = Math.min(len, skip);
            int nextSkip = skip - curSkip;
            if (nextSkip > 0) {
                skip = nextSkip;
            } else {
                skip = 0;
            }
            offset += curSkip;
            len -= curSkip;
            // would we go over our limit?
            /*
            if (bytesWritten + len > limit) {
                len -= (limit - bytesWritten);
                len = Math.max(0, len);
            }*/
            if (stream != null) {
                bytesWritten += len;
                stream.write(input, offset, len);
                md5.update(input, offset, len);
            }
        }
        public int skip;
        public int limit;
        public int bytesWritten;
        public MessageDigest md5;
        public String tempFilename;
        public String filename;
        public OutputStream stream;
        public String toString(){return stream.toString();}
    }

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

    private File attachmentsDirectory;

    private byte[] boundary;
    private int boundaryCount = 0;

    private Matcher boundaryMatcher;
    Section currentSection;
    public List<Section> sections;
    private int section;
    private Map<String, Object> json;
    List<Map.Entry<String, Object>> orderedAttachments;
    public int signalledAttachments;
    public int actualAttachments;

    MultipartAttachmentReader(byte[] boundary, String attachmentsDirectory) {

        // check we can save attachments
        this.attachmentsDirectory = new File(attachmentsDirectory);
        if (!(this.attachmentsDirectory.isDirectory() && this.attachmentsDirectory.canWrite())) {
            throw new IllegalArgumentException("The directory "+attachmentsDirectory+" does not exist or is not writable");
        }
        this.boundary = boundary;
        boundaryMatcher = new Matcher(boundary);
        currentSection = new Section();
        currentSection.stream = new ByteArrayOutputStream();
        sections = new ArrayList<Section>();
        orderedAttachments = new ArrayList<Map.Entry<String, Object>>();
        signalledAttachments = 0;
        actualAttachments = 0;
        section = 0;
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
//        System.out.println("+++"+new String(b)+"+++");
        // any new boundaries?

        if (boundaries.size() > 0) {
            for (int i=0; i<boundaries.size(); i++) {
                if (i == 0) {
                    // first one, write into current stream up to this point
                    int bOff = boundaries.get(i);
                    currentSection.write(b, off, bOff);
                }
                if (i == boundaries.size() -1) {
                    // last one, write to the end
                    int bOff = boundaries.get(i);
                    processExistingSectionAndStartNewSection();
                    currentSection.write(b, bOff, len - bOff);
                }
                else {
                    int bOff1 = boundaries.get(i);
                    int bOff2 = boundaries.get(i+1);
                    processExistingSectionAndStartNewSection();
                    currentSection.write(b, bOff1, bOff2 - bOff1);
                }
            }
        } else {
            // just write everything
            currentSection.write(b, off, len);
        }
    }
    private void processExistingSectionAndStartNewSection() throws FileNotFoundException, IOException {
        // if it was the body, check that it's in this format:
        // content-type: application/json
        //
        // <json payload>
        //
        // then build a document from the json and use the attachments dict to check sizes etc

        // if it was an attachment:
        // - check it's the right length
        // - truncate it if we wrote the boundary
        // - decompress if needed
        // - check the sha

        if (section == 1) {
            // json payload
            byte[] bodyBytes = ((ByteArrayOutputStream)currentSection.stream).toByteArray();
            String str = new String(bodyBytes);
            Pattern p = Pattern.compile("^\r\ncontent-type:( )+application/json\r\n\r\n", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
            java.util.regex.Matcher m = p.matcher(str);
            Boolean matches = m.find();
            if (matches) {
                // payload starts at the end of this match and goes up to the start of the boundary
                int start = m.end();
                int length = bodyBytes.length - boundary.length - start -2; // -2 for crlf
                // just for printing...
                //String payload = new String(bodyBytes, start, length);
                byte[] payload = new byte[length];
                System.arraycopy(bodyBytes, start, payload, 0, length);
                System.out.println(payload);
                json = JSONUtils.deserialize(payload);
                for (Map.Entry<String, Object> o: ((Map<String, Object>)json.get("_attachments")).entrySet()) {
                    //System.out.println("^^^ "+o);
                    orderedAttachments.add(o);
                    signalledAttachments++;
                }
            }
        } else if (section > 1 && section-2 < orderedAttachments.size()) {
            // get attachment for this offset
            Map.Entry<String, Object> att = orderedAttachments.get(section -2);
            System.out.println("Looking at "+att.getKey());
            actualAttachments++;
            currentSection.filename = att.getKey();
            int sectionLength = currentSection.bytesWritten - boundary.length - 2; // two crlfs (skipped) after boundary, one after content
            int expectedLength = (Integer)((Map<String, Object>)(att.getValue())).get("length");
            // check length
            if (expectedLength != sectionLength) {
                System.out.println("%%%%%%%%%%%ERROR "+expectedLength +", "+sectionLength);
            }
            // TODO check md-5
        }

        // clean up old file
        if (currentSection.tempFilename != null) {
            currentSection.stream.close();
            RandomAccessFile trunc = new RandomAccessFile(currentSection.tempFilename, "rw");
            trunc.getChannel().truncate(currentSection.bytesWritten - boundary.length - 2);
            trunc.close();
        }

        currentSection = new Section();
        if (section > 0) {
            // cook up a temp filename until we know what the real one is
//            currentSection.filename = new File(attachmentsDirectory, "tempfile"+UUID.randomUUID().toString()).toString();
            currentSection.tempFilename = new File(attachmentsDirectory, "tempfile"+section).toString();
            currentSection.stream = new FileOutputStream(currentSection.tempFilename);
            // skip crlfcrlf
            currentSection.skip = 4;
        } else {
            currentSection.stream = new ByteArrayOutputStream();
        }
        sections.add(currentSection);
        section++;
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
