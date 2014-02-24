package com.cloudant.sync.replication;

import com.cloudant.mazha.CouchConfig;
import com.cloudant.sync.datastore.Datastore;


/**
 * Represents a Replication.
 *
 * To fully describe a replication, it needs: remote CouchDB/Cloudant
 * and local datastore. The remote CouchDB/Cloudant is identified by
 * "couchConfig" and "CouchDbName".
 *
 * It also need to specify the replication type is PULL (remote -> local)
 * or Push (local -> remote).
 *
 * @see com.cloudant.mazha.CouchConfig
 * @see com.cloudant.sync.datastore.Datastore
 */
public class Replication {

    public enum Type {
        PULL,
        PUSH
    }

    public final Type type;
    public final CouchConfig couchConfig;
    public final String couchDbName;
    public final Datastore datastore;

    public Replication(Type type,
                       Datastore datastore,
                       CouchConfig couchConfig,
                       String dbName) {
        this.type = type;
        this.couchConfig = couchConfig;
        this.couchDbName = dbName;
        this.datastore = datastore;
    }

    public String getName() {
        if(this.type.equals(Replication.Type.PULL)) {
            return String.format("%s <-- %s", datastore.getDatastoreName(), couchDbName);
        } else {
            return String.format("%s <-- %s", couchDbName, datastore.getDatastoreName());
        }
    }

}
