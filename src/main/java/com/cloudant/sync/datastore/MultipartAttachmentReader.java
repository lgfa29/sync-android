package com.cloudant.sync.datastore;

import com.cloudant.sync.util.JSONUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import sun.misc.BASE64Encoder;

/**
 * Created by tomblench on 24/02/2014.
 */
public class MultipartAttachmentReader extends OutputStream {

    protected class Section {
        public Section() throws IOException {
            limit = -1;
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
            if (limit != -1 && bytesWritten + len > limit) {
                len = (limit - bytesWritten);
                len = Math.max(0, len);
            }
            if (stream != null) {
                bytesWritten += len;
                stream.write(input, offset, len);
                if (md5 != null) {
                    md5.update(input, offset, len);
                }
            }
        }
        public int skip;
        public int limit;
        public int bytesWritten;
        public MessageDigest md5;
        public String tempFilename;
        public String filename;
        public OutputStream stream;
        public String encoding;
        public Exception error;
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

    MultipartAttachmentReader(byte[] boundary, String attachmentsDirectory) throws IOException {

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
            }
        }

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
    private void processExistingSection() throws IOException {
        // if it was the body, check that it's in this format:
        // content-type: application/json
        //
        // <json payload>
        //
        // then build a document from the json and use the attachments dict to check sizes etc

        // if it was an attachment:
        // - check it's the right length
        // - check the md5
        // - decompress if needed

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
                byte[] payload = new byte[length];
                System.arraycopy(bodyBytes, start, payload, 0, length);
                System.out.println(payload);
                json = JSONUtils.deserialize(payload);
                for (Map.Entry<String, Object> o: ((Map<String, Object>)json.get("_attachments")).entrySet()) {
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
            int sectionLength = currentSection.bytesWritten; // two crlfs (skipped) after boundary, one after content
            int expectedLength = (Integer)(((Map<String, Object>)(att.getValue())).get("encoded_length") != null ?
                    ((Map<String, Object>)(att.getValue())).get("encoded_length") :
                    ((Map<String, Object>)(att.getValue())).get("length"));

            // check length
            if (expectedLength != sectionLength) {
                currentSection.error = new Exception("Actual length of " + sectionLength + " bytes did not match expected length of " + expectedLength + " bytes.");
                return;
            }

            // check MD5
            byte[] actualMd5 = currentSection.md5.digest();
            String actualMd5Str = "md5-"+(new BASE64Encoder().encode(actualMd5));
            String expectedMd5Str = (String)((Map<String, Object>)(att.getValue())).get("digest");
            if (!actualMd5Str.equals(expectedMd5Str)) {
                currentSection.error = new Exception("Actual MD5 of " + actualMd5Str + " did not match expected MD5 of " + expectedMd5Str + ".");
                return;
            }

            // unzip if it was encoded
            if ("gzip".equals(currentSection.encoding)) {
                try {
                    int bufSiz = 1024;
                    byte buf[] = new byte[bufSiz];
                    GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(currentSection.tempFilename));
                    FileOutputStream fos = new FileOutputStream(currentSection.tempFilename + "_uncomp");
                    int bytesRead;
                    while((bytesRead = gzis.read(buf)) != -1) {
                        fos.write(buf, 0, bytesRead);
                    }
                } catch (IOException ioe) {
                    currentSection.error = new Exception("IOException "+ioe+" whilst attempting to decompress gzip part");
                    return;
                }
            }

            // TODO move files to the blob store with the right names
        }
    }

    private void startNewSection() throws FileNotFoundException, IOException {

        currentSection = new Section();

        if (section > 0 && section-1 < orderedAttachments.size()) {
            // get attachment for this section
            Map.Entry<String, Object> att = orderedAttachments.get(section -1);
            String encoding = (String)((Map<String, Object>)(att.getValue())).get("encoding");
            boolean compressed = "gzip".equals(encoding);
            currentSection.encoding = encoding;
            int expectedLength = (Integer)(((Map<String, Object>)(att.getValue())).get("encoded_length") != null ?
                    ((Map<String, Object>)(att.getValue())).get("encoded_length") :
                    ((Map<String, Object>)(att.getValue())).get("length"));
            // cook up a temp filename until we know what the real one is
            currentSection.tempFilename = new File(attachmentsDirectory, "tempfile"+section).toString();
            currentSection.stream = new FileOutputStream(currentSection.tempFilename);
            currentSection.limit = expectedLength;
            // skip crlfcrlf
            currentSection.skip = 4;
        } else {
            currentSection.stream = new ByteArrayOutputStream();
        }
        sections.add(currentSection);
        section++;
    }

    private void processExistingSectionAndStartNewSection() throws FileNotFoundException, IOException {
        processExistingSection();
        startNewSection();
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
