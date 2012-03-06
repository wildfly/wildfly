package org.jboss.as.clustering.infinispan.subsystem;

import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.transaction.LockingMode;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jboss.as.clustering.subsystem.ObjectListAttributeDefinition;
import org.jboss.as.clustering.subsystem.ObjectTypeAttributeDefinition;
import org.jboss.as.clustering.subsystem.SimpleListAttributeDefinition;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Attributes used in setting up Infinispan configurations
 * To mark an attribute as required, mark it as not allowing null.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public interface CommonAttributes {

    SimpleAttributeDefinition ACQUIRE_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ACQUIRE_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.ACQUIRE_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(15000))
                    .build();
    SimpleAttributeDefinition ALIAS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ALIAS, ModelType.STRING, true)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleListAttributeDefinition ALIASES = SimpleListAttributeDefinition.Builder.of(ModelKeys.ALIASES, ALIAS).
            setAllowNull(true).
            build();
    SimpleAttributeDefinition ASYNC_MARSHALLING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ASYNC_MARSHALLING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.ASYNC_MARSHALLING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    SimpleAttributeDefinition BATCH_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.BATCH_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.BATCH_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(100))
                    .build();
    SimpleAttributeDefinition BATCHING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.BATCHING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.BATCHING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    SimpleAttributeDefinition CACHE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CACHE, ModelType.STRING, true)
                    .setXmlName(Attribute.CACHE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition CHUNK_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CHUNK_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.CHUNK_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(10000))
                    .build();
    SimpleAttributeDefinition CLASS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLASS, ModelType.STRING, false)
                    .setXmlName(Attribute.CLASS.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition CLUSTER =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CLUSTER, ModelType.STRING, true)
                    .setXmlName(Attribute.CLUSTER.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition CONCURRENCY_LEVEL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CONCURRENCY_LEVEL, ModelType.INT, true)
                    .setXmlName(Attribute.CONCURRENCY_LEVEL.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1000))
                    .build();
    SimpleAttributeDefinition DATA_SOURCE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DATASOURCE, ModelType.STRING, false)
                    .setXmlName(Attribute.DATASOURCE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    // make default-cache non required (AS7-3488)
    SimpleAttributeDefinition DEFAULT_CACHE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_CACHE, ModelType.STRING, true)
                    .setXmlName(Attribute.DEFAULT_CACHE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition DEFAULT_CACHE_CONTAINER =
            new SimpleAttributeDefinitionBuilder(ModelKeys.DEFAULT_CACHE_CONTAINER, ModelType.STRING, false)
                    .setXmlName(Attribute.DEFAULT_CACHE_CONTAINER.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    // this was removed?
    SimpleAttributeDefinition EAGER_LOCKING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.EAGER_LOCKING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.EAGER_LOCKING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    // enabled (used in state transfer, rehashing)
    SimpleAttributeDefinition ENABLED =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ENABLED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.ENABLED.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();
    SimpleAttributeDefinition EVICTION_EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.EVICTION_EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.EVICTION_EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition FETCH_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.FETCH_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.FETCH_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(100))
                    .build();
    SimpleAttributeDefinition FETCH_STATE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.FETCH_STATE, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.FETCH_STATE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();
    SimpleAttributeDefinition INDEXING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.INDEXING, ModelType.STRING, true)
                    .setXmlName(Attribute.INDEXING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<Indexing>(Indexing.class, true, false))
                    .setDefaultValue(new ModelNode().set(Indexing.NONE.name()))
                    .build();
    SimpleAttributeDefinition INTERVAL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.INTERVAL, ModelType.LONG, true)
                    .setXmlName(Attribute.INTERVAL.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(5000))
                    .build();
    SimpleAttributeDefinition ISOLATION =
            new SimpleAttributeDefinitionBuilder(ModelKeys.ISOLATION, ModelType.STRING, true)
                    .setXmlName(Attribute.ISOLATION.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<IsolationLevel>(IsolationLevel.class, true, false))
                    .setDefaultValue(new ModelNode().set(IsolationLevel.REPEATABLE_READ.name()))
                    .build();
    SimpleAttributeDefinition JNDI_NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.JNDI_NAME, ModelType.STRING, true)
                    .setXmlName(Attribute.JNDI_NAME.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition L1_LIFESPAN =
            new SimpleAttributeDefinitionBuilder(ModelKeys.L1_LIFESPAN, ModelType.LONG, true)
                    .setXmlName(Attribute.L1_LIFESPAN.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(600000))
                    .build();
    SimpleAttributeDefinition LIFESPAN =
            new SimpleAttributeDefinitionBuilder(ModelKeys.LIFESPAN, ModelType.LONG, true)
                    .setXmlName(Attribute.LIFESPAN.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(-1))
                    .build();
    SimpleAttributeDefinition LISTENER_EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.LISTENER_EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.LISTENER_EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition LOCKING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.LOCKING, ModelType.STRING, true)
                    .setXmlName(Attribute.LOCKING.getLocalName())
                    .setAllowExpression(false)
                    .setValidator(new EnumValidator<LockingMode>(LockingMode.class, true, false))
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(LockingMode.OPTIMISTIC.name()))
                    .build();
    SimpleAttributeDefinition LOCK_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.LOCK_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.LOCK_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(240000))
                    .build();
    SimpleAttributeDefinition MACHINE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MACHINE, ModelType.STRING, true)
                    .setXmlName(Attribute.MACHINE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition MAX_ENTRIES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MAX_ENTRIES, ModelType.INT, true)
                    .setXmlName(Attribute.MAX_ENTRIES.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(10000))
                    .build();
    SimpleAttributeDefinition MAX_IDLE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MAX_IDLE, ModelType.LONG, true)
                    .setXmlName(Attribute.MAX_IDLE.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(-1))
                    .build();
    // cache mode requited, txn mode not
    SimpleAttributeDefinition MODE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.MODE, ModelType.STRING, true)
                    .setXmlName(Attribute.MODE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<TransactionMode>(TransactionMode.class, true, false))
                    .setDefaultValue(new ModelNode().set(TransactionMode.NONE.name()))
                    .build();
    SimpleAttributeDefinition NAME =
            new SimpleAttributeDefinitionBuilder(ModelKeys.NAME, ModelType.STRING, true)
                    .setXmlName(Attribute.NAME.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition OWNERS =
            new SimpleAttributeDefinitionBuilder(ModelKeys.OWNERS, ModelType.INT, true)
                    .setXmlName(Attribute.OWNERS.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(2))
                    .build();
    SimpleAttributeDefinition PASSIVATION =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PASSIVATION, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.PASSIVATION.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();
    SimpleAttributeDefinition PATH =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PATH, ModelType.STRING, true)
                    .setXmlName(Attribute.PATH.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition PREFIX =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PREFIX, ModelType.STRING, true)
                    .setXmlName(Attribute.PREFIX.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
//                   .setDefaultValue(new ModelNode().set("ispn_bucket"))
//                   .setDefaultValue(new ModelNode().set("ispn_entry"))
                    .build();
    SimpleAttributeDefinition PRELOAD =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PRELOAD, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.PRELOAD.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    SimpleAttributeDefinition PROPERTY = new SimpleAttributeDefinition(ModelKeys.PROPERTY, ModelType.PROPERTY, true);
    SimpleListAttributeDefinition PROPERTIES = SimpleListAttributeDefinition.Builder.of(ModelKeys.PROPERTIES, PROPERTY).
            setAllowNull(true).
            build();
    SimpleListAttributeDefinition INDEXING_PROPERTIES = SimpleListAttributeDefinition.Builder.of(ModelKeys.INDEXING_PROPERTIES, PROPERTY).
            setAllowNull(true).
            build();
    SimpleAttributeDefinition PURGE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.PURGE, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.PURGE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();
    SimpleAttributeDefinition QUEUE_FLUSH_INTERVAL =
            new SimpleAttributeDefinitionBuilder(ModelKeys.QUEUE_FLUSH_INTERVAL, ModelType.LONG, true)
                    .setXmlName(Attribute.QUEUE_FLUSH_INTERVAL.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(10))
                    .build();
    SimpleAttributeDefinition QUEUE_SIZE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.QUEUE_SIZE, ModelType.INT, true)
                    .setXmlName(Attribute.QUEUE_SIZE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(0))
                    .build();
    SimpleAttributeDefinition RACK =
            new SimpleAttributeDefinitionBuilder(ModelKeys.RACK, ModelType.STRING, true)
                    .setXmlName(Attribute.RACK.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition RELATIVE_TO =
            new SimpleAttributeDefinitionBuilder(ModelKeys.RELATIVE_TO, ModelType.STRING, true)
                    .setXmlName(Attribute.RELATIVE_TO.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(ServerEnvironment.SERVER_DATA_DIR))
                    .build();
    SimpleAttributeDefinition REMOTE_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.REMOTE_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.REMOTE_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(17500))
                    .build();
    SimpleAttributeDefinition REPLICATION_QUEUE_EXECUTOR =
            new SimpleAttributeDefinitionBuilder(ModelKeys.REPLICATION_QUEUE_EXECUTOR, ModelType.STRING, true)
                    .setXmlName(Attribute.REPLICATION_QUEUE_EXECUTOR.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition SHARED =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SHARED, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.SHARED.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    SimpleAttributeDefinition SINGLETON =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SINGLETON, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.SINGLETON.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    SimpleAttributeDefinition SITE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SITE, ModelType.STRING, true)
                    .setXmlName(Attribute.SITE.getLocalName())
                    .setAllowExpression(true)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition SOCKET_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.SOCKET_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(60000))
                    .build();
    // if stack is null, use default stack
    SimpleAttributeDefinition STACK =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STACK, ModelType.STRING, true)
                    .setXmlName(Attribute.STACK.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition START =
            new SimpleAttributeDefinitionBuilder(ModelKeys.START, ModelType.STRING, true)
                    .setXmlName(Attribute.START.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<StartMode>(StartMode.class, true, false))
                    .setDefaultValue(new ModelNode().set(StartMode.LAZY.name()))
                    .build();
    SimpleAttributeDefinition STOP_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STOP_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.STOP_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(30000))
                    .build();
    SimpleAttributeDefinition EVICTION_STRATEGY =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STRATEGY, ModelType.STRING, true)
                    .setXmlName(Attribute.STRATEGY.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setValidator(new EnumValidator<EvictionStrategy>(EvictionStrategy.class, true, false))
                    .setDefaultValue(new ModelNode().set(EvictionStrategy.NONE.name()))
                    .build();
    SimpleAttributeDefinition STRIPING =
            new SimpleAttributeDefinitionBuilder(ModelKeys.STRIPING, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.STRIPING.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(false))
                    .build();
    SimpleAttributeDefinition TCP_NO_DELAY =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TCP_NO_DELAY, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.TCP_NO_DELAY.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();
    // timeout (used in state transfer, rehashing)
    SimpleAttributeDefinition TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(60000))
                    .build();
    SimpleAttributeDefinition TYPE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TYPE, ModelType.STRING, true)
                    .setXmlName(Attribute.TYPE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
//                    .setDefaultValue(new ModelNode().set("BINARY"))
//                    .setDefaultValue(new ModelNode().set("BIGINT"))
                    .build();
    SimpleAttributeDefinition VALUE =
            new SimpleAttributeDefinitionBuilder("value", ModelType.STRING, false)
                    .setXmlName("value")
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    SimpleAttributeDefinition VIRTUAL_NODES =
            new SimpleAttributeDefinitionBuilder(ModelKeys.VIRTUAL_NODES, ModelType.INT, true)
                    .setXmlName(Attribute.VIRTUAL_NODES.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(1))
                    .build();

    AttributeDefinition[] CACHE_CONTAINER_ATTRIBUTES = {DEFAULT_CACHE, ALIASES, JNDI_NAME, START, LISTENER_EXECUTOR, EVICTION_EXECUTOR, REPLICATION_QUEUE_EXECUTOR};
    AttributeDefinition[] TRANSPORT_ATTRIBUTES = {STACK, CLUSTER, EXECUTOR, LOCK_TIMEOUT, SITE, RACK, MACHINE};

    AttributeDefinition[] CACHE_ATTRIBUTES = { START, BATCHING, INDEXING, JNDI_NAME};
    AttributeDefinition[] CLUSTERED_CACHE_ATTRIBUTES = { ASYNC_MARSHALLING, ClusteredCacheAdd.MODE, QUEUE_SIZE, QUEUE_FLUSH_INTERVAL, REMOTE_TIMEOUT};
    AttributeDefinition[] DISTRIBUTED_CACHE_ATTRIBUTES = {OWNERS, VIRTUAL_NODES, L1_LIFESPAN};

    AttributeDefinition[] LOCKING_ATTRIBUTES = {ISOLATION, STRIPING, ACQUIRE_TIMEOUT, CONCURRENCY_LEVEL};
    AttributeDefinition[] TRANSACTION_ATTRIBUTES = {MODE, STOP_TIMEOUT, LOCKING};
    AttributeDefinition[] EVICTION_ATTRIBUTES = {EVICTION_STRATEGY, MAX_ENTRIES};
    AttributeDefinition[] EXPIRATION_ATTRIBUTES = {MAX_IDLE, LIFESPAN, INTERVAL};
    AttributeDefinition[] STATE_TRANSFER_ATTRIBUTES = {ENABLED, TIMEOUT, CHUNK_SIZE};

    // complex attribute definitions (helpers for now to create descriptions)
    ObjectTypeAttributeDefinition TRANSPORT_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.TRANSPORT, TRANSPORT_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("transport").
            build();

    ObjectTypeAttributeDefinition LOCKING_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.LOCKING, LOCKING_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("locking").
            build();
    ObjectTypeAttributeDefinition TRANSACTION_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.TRANSACTION, TRANSACTION_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("transaction").
            build();
    ObjectTypeAttributeDefinition EVICTION_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.EVICTION, EVICTION_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("eviction").
            build();
    ObjectTypeAttributeDefinition EXPIRATION_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.EXPIRATION, EXPIRATION_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("expiration").
            build();
    ObjectTypeAttributeDefinition STATE_TRANSFER_OBJECT = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.STATE_TRANSFER, STATE_TRANSFER_ATTRIBUTES).
            setAllowNull(true).
            setSuffix("state-transfer").
            build();

    // jdbc store
    SimpleAttributeDefinition COLUMN_NAME = new SimpleAttributeDefinition("name", ModelType.STRING, true);
    SimpleAttributeDefinition COLUMN_TYPE = new SimpleAttributeDefinition("type", ModelType.STRING, true);
    ObjectTypeAttributeDefinition ID_COLUMN = ObjectTypeAttributeDefinition.
            Builder.of("id-column", COLUMN_NAME, COLUMN_TYPE).
            setAllowNull(true).
            setSuffix("column").
            build();
    ObjectTypeAttributeDefinition DATA_COLUMN = ObjectTypeAttributeDefinition.
            Builder.of("data-column", COLUMN_NAME, COLUMN_TYPE).
            setAllowNull(true).
            setSuffix("column").
            build();
    ObjectTypeAttributeDefinition TIMESTAMP_COLUMN = ObjectTypeAttributeDefinition.
            Builder.of("timestamp-column", COLUMN_NAME, COLUMN_TYPE).
            setAllowNull(true).
            setSuffix("column").
            build();
    ObjectTypeAttributeDefinition ENTRY_TABLE = ObjectTypeAttributeDefinition.
            Builder.of("entry-table", PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN).
            setAllowNull(true).
            setSuffix("table").
            build();
    ObjectTypeAttributeDefinition BUCKET_TABLE = ObjectTypeAttributeDefinition.
            Builder.of("bucket-table", PREFIX, BATCH_SIZE, FETCH_SIZE, ID_COLUMN, DATA_COLUMN, TIMESTAMP_COLUMN).
            setAllowNull(true).
            setSuffix("table").
            build();

    // remote store
    SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING = new SimpleAttributeDefinition("outbound-socket-binding", ModelType.STRING, true);
    ObjectTypeAttributeDefinition REMOTE_SERVER = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.REMOTE_SERVER, OUTBOUND_SOCKET_BINDING).
            setAllowNull(true).
            setSuffix("remote-server").
            build();
    ObjectListAttributeDefinition REMOTE_SERVERS = ObjectListAttributeDefinition.Builder.of(ModelKeys.REMOTE_SERVERS, REMOTE_SERVER).
            setAllowNull(true).
            build();

    AttributeDefinition[] COMMON_STORE_ATTRIBUTES = {SHARED, PRELOAD, PASSIVATION, FETCH_STATE, PURGE, SINGLETON, PROPERTIES};
    AttributeDefinition[] STORE_ATTRIBUTES = {CLASS};
    AttributeDefinition[] FILE_STORE_ATTRIBUTES = {RELATIVE_TO, PATH};
    AttributeDefinition[] JDBC_STORE_ATTRIBUTES = {DATA_SOURCE, ENTRY_TABLE, BUCKET_TABLE};
    AttributeDefinition[] REMOTE_STORE_ATTRIBUTES = {CACHE, TCP_NO_DELAY, SOCKET_TIMEOUT, REMOTE_SERVERS};
}
