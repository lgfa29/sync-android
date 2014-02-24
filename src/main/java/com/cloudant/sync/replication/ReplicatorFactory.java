/**
 * Copyright (c) 2013 Cloudant, Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.sync.replication;

import com.cloudant.sync.datastore.Datastore;

import java.net.URI;

/**
 * <p>Factory for {@link Replicator} objects.</p>
 *
 * <p>The {@code source} or {@code target} {@link URI} parameters used in the
 * methods below must include:</p>
 *
 * <pre>
 *   protocol://[username:password@]host[:port]/database_name
 * </pre>
 *
 * <p><em>protocol</em>, <em>host</em> and <em>database_name</em> are required.
 * If no <em>port</em> is provided, the default for <em>protocol</em> is used.
 * Using a <em>database_name</em> containing a {@code /} is not supported.</p>
 */
public class ReplicatorFactory {

    /**
     * <p>Creates a {@link Replicator} object setup to replicate changes from
     * the local datastore to a remote database when {@code replication.type}
     * is {@code Type.PUll}, or from a remote database to the local
     * datastore when {@code replication.type} is {@code Type.PUSH}</p>
     *
     * @param replication instance of {@link Replication}, it includes all
     *                    information describes a replication: type: pull
     *                    or push, local datastore, remote Couchdb/Cloudant
     *                    info.
     *
     * @return a {@link com.cloudant.sync.replication.Replicator} instance
     *         which can be use to start and stop the replication
     */
    public static Replicator oneway(Replication replication) {
        return new BasicReplicator(replication);
    }
}