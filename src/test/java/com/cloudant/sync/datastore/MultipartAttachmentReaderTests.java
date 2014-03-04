package com.cloudant.sync.datastore;

import junit.framework.Assert;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by tomblench on 27/02/2014.
 */
public class MultipartAttachmentReaderTests {

    @Test
    public void TestReader() throws FileNotFoundException, IOException {

        int[] sizes = {1,4,5,7,12,127,1270};
        //int[] sizes = {1};
        //int[] sizes = {1270};

        for (int i=0;i<sizes.length;i++) {
            MultipartAttachmentReader mpr = new MultipartAttachmentReader("--nftjykeoeiyhopgldlwuzaimzahcdvlh".getBytes(), "/tmp");
            File f = new File("/Users/tomblench/testoffsets3");
//            MultipartAttachmentReader mpr = new MultipartAttachmentReader("--vusjhiawhakmyunbewwympozpvpmufyn".getBytes());
//            File f = new File("/Users/tomblench/testoffsets1");


            FileInputStream fis = new FileInputStream(f);
            int bufSize = sizes[i];
            byte[] buf = new byte[bufSize];
            int nRead = 0;
            while ((nRead = fis.read(buf)) > 0) {
                mpr.write(buf, 0, nRead);
            }
            System.out.println("bufsiz "+sizes[i]);
            Assert.assertEquals(1002, mpr.getBoundaryCount());
            Assert.assertEquals(1002, mpr.sections.size());
            Assert.assertEquals(mpr.actualAttachments, mpr.signalledAttachments);

            System.out.println(mpr.sections);
        }
    }

}
