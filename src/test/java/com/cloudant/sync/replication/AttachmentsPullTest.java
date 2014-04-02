package com.cloudant.sync.replication;

import com.cloudant.mazha.Response;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Created by tomblench on 26/03/2014.
 */
public class AttachmentsPullTest extends ReplicationTestBase {

    @Before
    public void setUp() throws Exception {
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }

    @Test
    public void pullRevisionsWithAttachments() {
        createRevisionAndAttachment();
        try {
            pull();
        } catch (Exception e) {
            System.out.println("error");
        }
        // TODO get attachment and assert on content, name etc
    }

    private void createRevisionAndAttachment() {
        Bar bar = new Bar();
        bar.setName("Tom");
        bar.setAge(34);


        Response res = remoteDb.create(bar);
        bar = remoteDb.get(Bar.class, res.getId());

        String id = res.getId();
        String rev = res.getRev();
        remoteDb.getCouchClient().putAttachmentStream(id, rev, "att1", "This is an attachment");
    }

    private void pull() throws Exception {
        TestStrategyListener listener = new TestStrategyListener();
        BasicPullStrategy pull = new BasicPullStrategy(this.createPullReplication());
        pull.getEventBus().register(listener);

        Thread t = new Thread(pull);
        t.start();
        t.join();
        Assert.assertTrue(listener.finishCalled);
        Assert.assertFalse(listener.errorCalled);
    }



}
