package com.cloudant.sync.datastore;

import com.cloudant.sync.util.Misc;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by tomblench on 12/03/2014.
 */
public class AttachmentTest extends BasicDatastoreTestBase {

    @Test
    public void setAndGetAttachmentsTest() {
        String attachmentName = "attachment_1.txt";
        BasicDocumentRevision rev_1 = datastore.createDocument(bodyOne);
        File f = new File("fixture", attachmentName);
        UnsavedFileAttachment att = new UnsavedFileAttachment(f, "text/plain");
        List<UnsavedFileAttachment> atts = new ArrayList<UnsavedFileAttachment>();
        atts.add(att);
        DocumentRevision newRevision = null;
        try {
            newRevision = datastore.setAttachments(rev_1, atts);
        } catch (ConflictException ce){
            Assert.fail("ConflictException thrown: "+ce);
        } catch (IOException ioe) {
            Assert.fail("IOException thrown: "+ioe);
        }
        // get attachment...
        try {
            byte[] expectedSha1 = Misc.getSha1(new FileInputStream(f));

            SavedAttachment savedAtt = (SavedAttachment) datastore.getAttachment(newRevision, attachmentName);
            Assert.assertArrayEquals(expectedSha1, savedAtt.key);

            SavedAttachment savedAtt2 = (SavedAttachment) datastore.getAttachments(newRevision).get(0);
            Assert.assertArrayEquals(expectedSha1, savedAtt2.key);
        } catch (FileNotFoundException fnfe) {
            Assert.fail("FileNotFoundException thrown "+fnfe);
        }
    }

    @Test
    public void createDeleteAttachmentsTest() {

        String attachmentName = "attachment_1.txt";
        BasicDocumentRevision rev_1 = datastore.createDocument(bodyOne);
        File f = new File("fixture", attachmentName);
        UnsavedFileAttachment att = new UnsavedFileAttachment(f, "text/plain");
        List<UnsavedFileAttachment> atts = new ArrayList<UnsavedFileAttachment>();
        atts.add(att);
        DocumentRevision rev2 = null;
        try {
            rev2 = datastore.setAttachments(rev_1, atts);
            Assert.assertNotNull("Revision null", rev2);
        } catch (ConflictException ce){
            Assert.fail("ConflictException thrown: "+ce);
        } catch (IOException ioe) {
            Assert.fail("IOException thrown: "+ioe);
        }

        DocumentRevision rev3 = null;
        try {
            rev3 = datastore.removeAttachment(rev2, attachmentName);
            Assert.assertNotNull("Revision null", rev3);
        } catch (Exception e) {
            Assert.fail("Exception thrown: "+e);
        }

    }

    @Test
    public void listAttachmentsTests() {
        String attachmentName = "attachment_1.txt";
        BasicDocumentRevision rev_1 = datastore.createDocument(bodyOne);
        File f = new File("fixture", attachmentName);
        UnsavedFileAttachment att = new UnsavedFileAttachment(f, "text/plain");
        List<UnsavedFileAttachment> atts = new ArrayList<UnsavedFileAttachment>();
        atts.add(att);
        DocumentRevision newRevision = null;
        try {
            newRevision = datastore.setAttachments(rev_1, atts);
        } catch (ConflictException ce){
            Assert.fail("ConflictException thrown: "+ce);
        } catch (IOException ioe) {
            Assert.fail("IOException thrown: "+ioe);
        }
//        datastore.attachmentsForRevision()
    }



}
