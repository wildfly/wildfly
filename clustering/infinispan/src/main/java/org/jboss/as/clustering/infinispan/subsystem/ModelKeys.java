/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.infinispan.subsystem;

/**
 * @author Paul Ferraro
 */
public class ModelKeys {
    static final String ACQUIRE_TIMEOUT = "acquire-timeout";
    static final String ALIAS = "alias";
    static final String ALIASES = "aliases";
    static final String BATCH_SIZE = "batch-size";
    static final String BATCHING = "batching";
    static final String BUCKET_TABLE = "bucket-table";
    static final String CACHE = "cache";
    // static final String CACHE_MODE = "cache-mode";
    static final String CACHE_CONTAINER = "cache-container";
    static final String CHUNK_SIZE = "chunk-size";
    static final String CLASS = "class";
    static final String CONCURRENCY_LEVEL = "concurrency-level";
    static final String DATA_COLUMN = "data-column";
    static final String DATASOURCE = "datasource";
    static final String DEFAULT_CACHE = "default-cache";
    static final String DEFAULT_CACHE_CONTAINER = "default-cache-container";
    static final String DISTRIBUTED_CACHE = "distributed-cache";
    static final String EAGER_LOCKING = "eager-locking";
    static final String ENABLED = "enabled";
    static final String ENTRY_TABLE = "entry-table";
    static final String EVICTION = "eviction";
    static final String EVICTION_NAME = "EVICTION";
    static final String EVICTION_EXECUTOR = "eviction-executor";
    static final String EXECUTOR = "executor";
    static final String EXPIRATION = "expiration";
    static final String EXPIRATION_NAME = "EXPIRATION";
    static final String FETCH_SIZE = "fetch-size";
    static final String FETCH_STATE = "fetch-state";
    static final String FILE_STORE = "file-store";
    static final String FILE_STORE_NAME = "FILE_STORE";
    static final String ID_COLUMN = "id-column";
    static final String INDEXING = "indexing";
    static final String INTERVAL = "interval";
    static final String INVALIDATION_CACHE = "invalidation-cache";
    static final String ISOLATION = "isolation";
    static final String JDBC_STORE = "jdbc-store";
    static final String JDBC_STORE_NAME = "JDBC_STORE";
    static final String JNDI_NAME = "jndi-name";
    static final String L1_LIFESPAN = "l1-lifespan";
    static final String LIFESPAN = "lifespan";
    static final String LISTENER_EXECUTOR = "listener-executor";
    static final String LOCAL_CACHE = "local-cache";
    static final String LOCK_TIMEOUT = "lock-timeout";
    static final String LOCKING = "locking";
    static final String LOCKING_NAME = "LOCKING";
    static final String MACHINE = "machine";
    static final String MAX_ENTRIES = "max-entries";
    static final String MAX_IDLE = "max-idle";
    static final String MODE = "mode";
    static final String NAME = "name";
    static final String OUTBOUND_SOCKET_BINDING = "outbound-socket-binding";
    static final String OWNERS = "owners";
    static final String PASSIVATION = "passivation";
    static final String PATH = "path";
    static final String PREFIX = "prefix";
    static final String PRELOAD = "preload";
    static final String PROPERTY = "property";
    static final String PROPERTIES = "properties";
    static final String PURGE = "purge";
    static final String QUEUE_FLUSH_INTERVAL = "queue-flush-interval";
    static final String QUEUE_SIZE = "queue-size";
    static final String RACK = "rack";
    static final String RELATIVE_TO = "relative-to";
    static final String REMOTE_SERVER = "remote-server";
    static final String REMOTE_SERVERS = "remote-servers";
    static final String REMOTE_STORE_NAME = "REMOTE_STORE";
    static final String REMOTE_STORE = "remote-store";
    static final String REMOTE_TIMEOUT = "remote-timeout";
    static final String REPLICATED_CACHE = "replicated-cache";
    static final String REPLICATION_QUEUE_EXECUTOR = "replication-queue-executor";
    static final String SHARED = "shared";
    static final String SINGLETON = "singleton";
    static final String SITE = "site";
    static final String SOCKET_TIMEOUT = "socket-timeout";
    static final String STACK = "stack";
    static final String START = "start";
    static final String STATE_TRANSFER = "state-transfer";
    static final String STATE_TRANSFER_NAME = "STATE_TRANSFER";
    static final String STOP_TIMEOUT = "stop-timeout";
    static final String STORE = "store";
    static final String STORE_NAME = "STORE";
    static final String STRATEGY = "strategy";
    static final String STRIPING = "striping";
    static final String TCP_NO_DELAY = "tcp-no-delay";
    static final String TIMEOUT = "timeout";
    static final String TIMESTAMP_COLUMN = "timestamp-column";
    static final String TRANSACTION = "transaction";
    static final String TRANSACTION_NAME = "TRANSACTION";
    static final String TRANSPORT = "transport";
    static final String TRANSPORT_NAME = "TRANSPORT";
    static final String TYPE = "type";
    static final String VIRTUAL_NODES = "virtual-nodes";
    static final String WAIT = "wait";
}
