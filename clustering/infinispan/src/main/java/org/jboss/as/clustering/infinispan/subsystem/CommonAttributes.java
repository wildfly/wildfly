package org.jboss.as.clustering.infinispan.subsystem;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Attributes used in setting up Infinispan configurations
 *
 * To mark an attribute as required, mark it as not allowing null.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    long longDefault = 0 ;
    boolean booleanDefault = false ;
    int intDefault = 0 ;

    SimpleAttributeDefinition ACQUIRE_TIMEOUT = new SimpleAttributeDefinition(ModelKeys.ACQUIRE_TIMEOUT,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition ALIAS = new SimpleAttributeDefinition(ModelKeys.ALIAS, ModelType.LIST, true);
    SimpleAttributeDefinition BATCH_SIZE = new SimpleAttributeDefinition(ModelKeys.BATCH_SIZE,
            new ModelNode().set(intDefault), ModelType.INT, true);
    SimpleAttributeDefinition BATCHING = new SimpleAttributeDefinition(ModelKeys.BATCHING,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition CHUNK_SIZE = new SimpleAttributeDefinition(ModelKeys.CHUNK_SIZE,
            new ModelNode().set(intDefault), ModelType.INT,  true);
    SimpleAttributeDefinition CLASS = new SimpleAttributeDefinition(ModelKeys.CLASS, ModelType.STRING, true);
    SimpleAttributeDefinition CONCURRENCY_LEVEL = new SimpleAttributeDefinition(ModelKeys.CONCURRENCY_LEVEL,
            new ModelNode().set(intDefault), ModelType.INT, true);
    SimpleAttributeDefinition DATA_SOURCE = new SimpleAttributeDefinition(ModelKeys.DATASOURCE, ModelType.STRING, true);
    SimpleAttributeDefinition DEFAULT_CACHE = new SimpleAttributeDefinition(ModelKeys.DEFAULT_CACHE, ModelType.STRING, false);
    SimpleAttributeDefinition DEFAULT_CACHE_CONTAINER = new SimpleAttributeDefinition(ModelKeys.DEFAULT_CACHE_CONTAINER, ModelType.STRING, false);
    SimpleAttributeDefinition EAGER_LOCKING = new SimpleAttributeDefinition(ModelKeys.EAGER_LOCKING,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    // enabled (used in state transfer, rehashing)
    SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinition(ModelKeys.ENABLED,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition EVICTION_EXECUTOR = new SimpleAttributeDefinition(ModelKeys.EVICTION_EXECUTOR, ModelType.STRING, true);
    SimpleAttributeDefinition EXECUTOR = new SimpleAttributeDefinition(ModelKeys.EXECUTOR, ModelType.STRING, true);
    SimpleAttributeDefinition FETCH_SIZE = new SimpleAttributeDefinition(ModelKeys.FETCH_SIZE,
            new ModelNode().set(intDefault), ModelType.INT, true);
    SimpleAttributeDefinition FETCH_STATE = new SimpleAttributeDefinition(ModelKeys.FETCH_STATE,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition INDEXING = new SimpleAttributeDefinition(ModelKeys.INDEXING, ModelType.STRING, true);
    SimpleAttributeDefinition INTERVAL = new SimpleAttributeDefinition(ModelKeys.INTERVAL,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition ISOLATION = new SimpleAttributeDefinition(ModelKeys.ISOLATION, ModelType.STRING, true);
    SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinition(ModelKeys.JNDI_NAME, ModelType.STRING, true);
    SimpleAttributeDefinition L1_LIFESPAN = new SimpleAttributeDefinition(ModelKeys.L1_LIFESPAN,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition LIFESPAN = new SimpleAttributeDefinition(ModelKeys.LIFESPAN,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition LISTENER_EXECUTOR = new SimpleAttributeDefinition(ModelKeys.LISTENER_EXECUTOR, ModelType.STRING, true);
    SimpleAttributeDefinition LOCKING = new SimpleAttributeDefinition(ModelKeys.LOCKING, ModelType.STRING, true);
    SimpleAttributeDefinition LOCK_TIMEOUT = new SimpleAttributeDefinition(ModelKeys.LOCK_TIMEOUT,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition MACHINE = new SimpleAttributeDefinition(ModelKeys.MACHINE, ModelType.STRING, true);
    SimpleAttributeDefinition MAX_ENTRIES = new SimpleAttributeDefinition(ModelKeys.MAX_ENTRIES,
            new ModelNode().set(intDefault), ModelType.INT, true);
    SimpleAttributeDefinition MAX_IDLE = new SimpleAttributeDefinition(ModelKeys.MAX_IDLE,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition MODE = new SimpleAttributeDefinition(ModelKeys.MODE, ModelType.STRING, true);
    // name String
    // namespace String
    SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING = new SimpleAttributeDefinition(ModelKeys.OUTBOUND_SOCKET_BINDING, ModelType.STRING, true);
    SimpleAttributeDefinition OWNERS = new SimpleAttributeDefinition(ModelKeys.OWNERS,
            new ModelNode().set(intDefault), ModelType.INT, true);
    SimpleAttributeDefinition PASSIVATION = new SimpleAttributeDefinition(ModelKeys.PASSIVATION,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition PATH = new SimpleAttributeDefinition(ModelKeys.PATH, ModelType.STRING, true);
    SimpleAttributeDefinition PREFIX = new SimpleAttributeDefinition(ModelKeys.PREFIX, ModelType.STRING, true);
    SimpleAttributeDefinition PRELOAD = new SimpleAttributeDefinition(ModelKeys.PRELOAD,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition PURGE = new SimpleAttributeDefinition(ModelKeys.PURGE,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition QUEUE_FLUSH_INTERVAL = new SimpleAttributeDefinition(ModelKeys.QUEUE_FLUSH_INTERVAL,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition QUEUE_SIZE = new SimpleAttributeDefinition(ModelKeys.QUEUE_SIZE,
            new ModelNode().set(intDefault), ModelType.INT, true);
    SimpleAttributeDefinition RACK = new SimpleAttributeDefinition(ModelKeys.RACK, ModelType.STRING, true);
    SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinition(ModelKeys.RELATIVE_TO, ModelType.STRING, true);
    SimpleAttributeDefinition REMOTE_TIMEOUT = new SimpleAttributeDefinition(ModelKeys.REMOTE_TIMEOUT,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition REPLICATION_QUEUE_EXECUTOR = new SimpleAttributeDefinition(ModelKeys.REPLICATION_QUEUE_EXECUTOR, ModelType.STRING, true);
    SimpleAttributeDefinition SHARED = new SimpleAttributeDefinition(ModelKeys.SHARED,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition SINGLETON = new SimpleAttributeDefinition(ModelKeys.SINGLETON,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition SITE = new SimpleAttributeDefinition(ModelKeys.SITE, ModelType.STRING, true);
    SimpleAttributeDefinition SOCKET_TIMEOUT = new SimpleAttributeDefinition(ModelKeys.SOCKET_TIMEOUT,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition STACK = new SimpleAttributeDefinition(ModelKeys.STACK, ModelType.STRING, false);
    SimpleAttributeDefinition START = new SimpleAttributeDefinition(ModelKeys.START, ModelType.STRING, true);
    SimpleAttributeDefinition STOP_TIMEOUT = new SimpleAttributeDefinition(ModelKeys.STOP_TIMEOUT,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition STRATEGY = new SimpleAttributeDefinition(ModelKeys.STRATEGY, ModelType.STRING, true);
    SimpleAttributeDefinition STRIPING = new SimpleAttributeDefinition(ModelKeys.STRIPING,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    SimpleAttributeDefinition TCP_NO_DELAY = new SimpleAttributeDefinition(ModelKeys.TCP_NO_DELAY,
            new ModelNode().set(booleanDefault), ModelType.BOOLEAN,  true);
    // timeout (used in state transfer, rehashing)
    SimpleAttributeDefinition TIMEOUT = new SimpleAttributeDefinition(ModelKeys.TIMEOUT,
            new ModelNode().set(longDefault), ModelType.LONG,  true, MeasurementUnit.MILLISECONDS);
    SimpleAttributeDefinition TYPE = new SimpleAttributeDefinition(ModelKeys.TYPE, ModelType.STRING, true);
    SimpleAttributeDefinition VALUE = new SimpleAttributeDefinition("value", ModelType.STRING, false);
    SimpleAttributeDefinition VIRTUAL_NODES = new SimpleAttributeDefinition(ModelKeys.VIRTUAL_NODES,
            new ModelNode().set(intDefault), ModelType.INT, true);

    AttributeDefinition[] CACHE_CONTAINER_ATTRIBUTES = { DEFAULT_CACHE, JNDI_NAME, LISTENER_EXECUTOR, EVICTION_EXECUTOR, REPLICATION_QUEUE_EXECUTOR, ALIAS };
    AttributeDefinition[] TRANSPORT_ATTRIBUTES = { STACK, EXECUTOR, LOCK_TIMEOUT, SITE, RACK, MACHINE  };

    AttributeDefinition[] CACHE_ATTRIBUTES = { /* NAME, */ START, BATCHING, INDEXING, JNDI_NAME };
    AttributeDefinition[] CLUSTERED_CACHE_ATTRIBUTES = { MODE,  QUEUE_SIZE, QUEUE_FLUSH_INTERVAL, REMOTE_TIMEOUT };
    AttributeDefinition[] DISTRIBUTED_CACHE_ATTRIBUTES = { OWNERS, VIRTUAL_NODES, L1_LIFESPAN };

    AttributeDefinition[] LOCKING_ATTRIBUTES = { ISOLATION, STRIPING, ACQUIRE_TIMEOUT, CONCURRENCY_LEVEL  };
    AttributeDefinition[] TRANSACTION_ATTRIBUTES = { MODE, STOP_TIMEOUT, EAGER_LOCKING  };
    AttributeDefinition[] EVICTION_ATTRIBUTES = { STRATEGY, MAX_ENTRIES };
    AttributeDefinition[] EXPIRATION_ATTRIBUTES = { MAX_IDLE, LIFESPAN, INTERVAL };
    AttributeDefinition[] STORE_ATTRIBUTES = { SHARED, PRELOAD, PASSIVATION, FETCH_STATE, PURGE, SINGLETON /* PROPERTY */};
    AttributeDefinition[] FILESTORE_ATTRIBUTES = { RELATIVE_TO, PATH };
    AttributeDefinition[] JDBC_STORE_ATTRIBUTES = { DATA_SOURCE };
    AttributeDefinition[] REMOTE_ATTRIBUTES = { /* REMOTE_SERVER */ };

    AttributeDefinition[] STATE_TRANSFER_ATTRIBUTES = { ENABLED, TIMEOUT, CHUNK_SIZE };
}
