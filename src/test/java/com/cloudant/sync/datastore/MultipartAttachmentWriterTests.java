package com.cloudant.sync.datastore;

import com.cloudant.sync.util.TestUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

/**
 * Created by tomblench on 24/02/2014.
 */
public class MultipartAttachmentWriterTests {

    String documentOneFile = "fixture/document_1.json";
    String documentTwoFile = "fixture/document_2.json";


    String datastore_manager_dir;
    DatastoreManager datastoreManager;
    Datastore datastore = null;


    byte[] jsonData = null;
    DocumentBody bodyOne = null;

    @Before
    public void setUp() throws Exception {
        datastore_manager_dir = TestUtils.createTempTestingDir(this.getClass().getName());
        datastoreManager = new DatastoreManager(this.datastore_manager_dir);
        datastore = (this.datastoreManager.openDatastore(getClass().getSimpleName()));

        jsonData = "{\"body\":\"This is a body.\"}".getBytes();
        bodyOne = BasicDocumentBody.bodyWith(jsonData);
    }

    @After
    public void tearDown() throws Exception {
        TestUtils.deleteTempTestingDir(datastore_manager_dir);
    }

    @Test
    public void Test1() {
        try {
            // TODO some asserts etc
            DocumentRevision doc = datastore.createDocument(bodyOne);
            ArrayList<Attachment> attachments = new ArrayList<Attachment>();

            Attachment att0 = new Attachment();
            att0.name = "attachment0";
            att0.contentType = "image/jpeg";
            byte[] bytes = "this is some data".getBytes();
            att0.data = new ByteArrayInputStream(bytes);
            att0.length = bytes.length;

            attachments.add(att0);

            MultipartAttachmentWriter mpw = new MultipartAttachmentWriter(datastore, doc, attachments);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int chunkSize = 3;
            int amountRead = 0;

            do {
                byte buf[] = new byte[chunkSize];
                amountRead = mpw.read(buf);
                if (amountRead > 0) {
                    System.out.print(new String(buf, 0, amountRead));
                }
            } while(amountRead > 0);
        } catch (Exception e) {
            System.out.println("aarg "+e);
        }
    }

}
