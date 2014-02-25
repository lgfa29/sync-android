package com.cloudant.sync.attachments;

import com.cloudant.sync.datastore.BasicDocumentBody;
import com.cloudant.sync.datastore.ConflictException;
import com.cloudant.sync.datastore.Datastore;
import com.cloudant.sync.datastore.DatastoreManager;
import com.cloudant.sync.datastore.DatastoreTestBase;
import com.cloudant.sync.datastore.DocumentBody;
import com.cloudant.sync.datastore.DocumentRevision;
import com.cloudant.sync.sqlite.sqlite4java.SQLiteWrapper;
import com.cloudant.sync.util.TestUtils;
import com.sun.xml.internal.messaging.saaj.util.ByteInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.tools.ant.filters.StringInputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.util.ArrayList;

/**
 * Created by tomblench on 24/02/2014.
 */
public class WriterTests {

    String documentOneFile = "fixture/document_1.json";
    String documentTwoFile = "fixture/document_2.json";


    String datastore_manager_dir;
    DatastoreManager datastoreManager;
    Datastore datastore = null;


    byte[] jsonData = null;
    DocumentBody bodyOne = null;
    DocumentBody bodyTwo = null;



    @Before
    public void setUp() throws Exception {
        datastore_manager_dir = TestUtils.createTempTestingDir(this.getClass().getName());
        datastoreManager = new DatastoreManager(this.datastore_manager_dir);
        datastore = (this.datastoreManager.openDatastore(getClass().getSimpleName()));

        jsonData = FileUtils.readFileToByteArray(new File(documentOneFile));
        bodyOne = BasicDocumentBody.bodyWith(jsonData);

        jsonData = FileUtils.readFileToByteArray(new File(documentTwoFile));
        bodyTwo = BasicDocumentBody.bodyWith(jsonData);
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
            att0.data = new ByteArrayInputStream("this is some data".getBytes());

            attachments.add(att0);

            MultipartWriter mpw = new MultipartWriter(doc, attachments);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            int chunkSize = 3;
            int amountRead = 0;

            do {
                byte buf[] = new byte[chunkSize];
                amountRead = mpw.read(buf);
                  System.out.print(new String(buf, 0, amountRead));
            } while(amountRead > 0);
        } catch (Exception e) {
            System.out.println("aarg "+e);
        }


    }

}
