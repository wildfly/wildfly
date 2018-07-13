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

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.clustering.controller.Attribute;
import org.jboss.as.clustering.infinispan.subsystem.remote.ConnectionPoolResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.HotRodStoreResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.SecurityResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Enumerates the attributes used in the Infinispan subsystem schema.
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 RedHat Inc.
 * @author Tristan Tarrant
 */
public enum XMLAttribute {
    // must be first
    UNKNOWN(""),
    ACQUIRE_TIMEOUT(LockingResourceDefinition.Attribute.ACQUIRE_TIMEOUT),
    CAPACITY(OffHeapMemoryResourceDefinition.Attribute.CAPACITY),
    ALIASES(CacheContainerResourceDefinition.Attribute.ALIASES),
    @Deprecated ASYNC_MARSHALLING(ClusteredCacheResourceDefinition.DeprecatedAttribute.ASYNC_MARSHALLING),
    BACKUP_FAILURE_POLICY(BackupResourceDefinition.Attribute.FAILURE_POLICY),
    @Deprecated BATCH_SIZE(TableResourceDefinition.DeprecatedAttribute.BATCH_SIZE),
    @Deprecated BATCHING(CacheResourceDefinition.DeprecatedAttribute.BATCHING),
    BIAS_LIFESPAN(ScatteredCacheResourceDefinition.Attribute.BIAS_LIFESPAN),
    @Deprecated CACHE(RemoteStoreResourceDefinition.Attribute.CACHE),
    CAPACITY_FACTOR(DistributedCacheResourceDefinition.Attribute.CAPACITY_FACTOR),
    CHANNEL(JGroupsTransportResourceDefinition.Attribute.CHANNEL),
    CHUNK_SIZE(StateTransferResourceDefinition.Attribute.CHUNK_SIZE),
    CLASS(CustomStoreResourceDefinition.Attribute.CLASS),
    @Deprecated CLUSTER(JGroupsTransportResourceDefinition.DeprecatedAttribute.CLUSTER),
    CONCURRENCY_LEVEL(LockingResourceDefinition.Attribute.CONCURRENCY),
    CONSISTENT_HASH_STRATEGY(SegmentedCacheResourceDefinition.Attribute.CONSISTENT_HASH_STRATEGY),
    DATA_SOURCE(JDBCStoreResourceDefinition.Attribute.DATA_SOURCE),
    @Deprecated DATASOURCE(JDBCStoreResourceDefinition.DeprecatedAttribute.DATASOURCE),
    DEFAULT_CACHE(CacheContainerResourceDefinition.Attribute.DEFAULT_CACHE),
    @Deprecated DEFAULT_CACHE_CONTAINER("default-cache-container"),
    DIALECT(JDBCStoreResourceDefinition.Attribute.DIALECT),
    @Deprecated EAGER_LOCKING("eager-locking"),
    ENABLED(BackupResourceDefinition.Attribute.ENABLED),
    @Deprecated EVICTION_EXECUTOR(CacheContainerResourceDefinition.ExecutorAttribute.EVICTION),
    EVICTION_TYPE(BinaryMemoryResourceDefinition.Attribute.EVICTION_TYPE),
    @Deprecated EXECUTOR(JGroupsTransportResourceDefinition.ExecutorAttribute.TRANSPORT),
    FETCH_SIZE(TableResourceDefinition.Attribute.FETCH_SIZE),
    FETCH_STATE(StoreResourceDefinition.Attribute.FETCH_STATE),
    @Deprecated FLUSH_LOCK_TIMEOUT(StoreWriteBehindResourceDefinition.DeprecatedAttribute.FLUSH_LOCK_TIMEOUT),
    @Deprecated FLUSH_TIMEOUT("flush-timeout"),
    @Deprecated INDEXING(CacheResourceDefinition.DeprecatedAttribute.INDEXING),
    @Deprecated INDEX("index"),
    INTERVAL(ExpirationResourceDefinition.Attribute.INTERVAL),
    INVALIDATION_BATCH_SIZE(ScatteredCacheResourceDefinition.Attribute.INVALIDATION_BATCH_SIZE),
    ISOLATION(LockingResourceDefinition.Attribute.ISOLATION),
    @Deprecated JNDI_NAME(CacheContainerResourceDefinition.DeprecatedAttribute.JNDI_NAME),
    KEEPALIVE_TIME(ThreadPoolResourceDefinition.values()[0].getKeepAliveTime()),
    L1_LIFESPAN(DistributedCacheResourceDefinition.Attribute.L1_LIFESPAN),
    LIFESPAN(ExpirationResourceDefinition.Attribute.LIFESPAN),
    @Deprecated LISTENER_EXECUTOR(CacheContainerResourceDefinition.ExecutorAttribute.LISTENER),
    LOCK_TIMEOUT(JGroupsTransportResourceDefinition.Attribute.LOCK_TIMEOUT),
    LOCKING(TransactionResourceDefinition.Attribute.LOCKING),
    MACHINE("machine"),
    MAX_BATCH_SIZE(StoreResourceDefinition.Attribute.MAX_BATCH_SIZE),
    MAX_ENTRIES(ObjectMemoryResourceDefinition.DeprecatedAttribute.MAX_ENTRIES),
    MAX_IDLE(ExpirationResourceDefinition.Attribute.MAX_IDLE),
    MAX_THREADS(ThreadPoolResourceDefinition.values()[0].getMaxThreads()),
    MIN_THREADS(ThreadPoolResourceDefinition.values()[0].getMinThreads()),
    @Deprecated MODE(ClusteredCacheResourceDefinition.DeprecatedAttribute.MODE),
    MODIFICATION_QUEUE_SIZE(StoreWriteBehindResourceDefinition.Attribute.MODIFICATION_QUEUE_SIZE),
    MODULE(CacheContainerResourceDefinition.Attribute.MODULE),
    NAME(ModelDescriptionConstants.NAME),
    OUTBOUND_SOCKET_BINDING("outbound-socket-binding"),
    OWNERS(DistributedCacheResourceDefinition.Attribute.OWNERS),
    PASSIVATION(StoreResourceDefinition.Attribute.PASSIVATION),
    PATH(FileStoreResourceDefinition.Attribute.RELATIVE_PATH),
    PREFIX(StringTableResourceDefinition.Attribute.PREFIX),
    PRELOAD(StoreResourceDefinition.Attribute.PRELOAD),
    PURGE(StoreResourceDefinition.Attribute.PURGE),
    @Deprecated QUEUE_FLUSH_INTERVAL(ClusteredCacheResourceDefinition.DeprecatedAttribute.QUEUE_FLUSH_INTERVAL),
    @Deprecated QUEUE_SIZE(ClusteredCacheResourceDefinition.DeprecatedAttribute.QUEUE_SIZE),
    QUEUE_LENGTH(ThreadPoolResourceDefinition.values()[0].getQueueLength()),
    RACK("rack"),
    RELATIVE_TO(FileStoreResourceDefinition.Attribute.RELATIVE_TO),
    @Deprecated REMOTE_CACHE(BackupForResourceDefinition.Attribute.CACHE),
    @Deprecated REMOTE_SERVERS(RemoteStoreResourceDefinition.Attribute.SOCKET_BINDINGS),
    @Deprecated REMOTE_SITE(BackupForResourceDefinition.Attribute.SITE),
    REMOTE_TIMEOUT(ClusteredCacheResourceDefinition.Attribute.REMOTE_TIMEOUT),
    @Deprecated REPLICATION_QUEUE_EXECUTOR(CacheContainerResourceDefinition.ExecutorAttribute.REPLICATION_QUEUE),
    SEGMENTS(SegmentedCacheResourceDefinition.Attribute.SEGMENTS),
    SHARED(StoreResourceDefinition.Attribute.SHARED),
    @Deprecated SHUTDOWN_TIMEOUT(StoreWriteBehindResourceDefinition.DeprecatedAttribute.SHUTDOWN_TIMEOUT),
    SINGLETON(StoreResourceDefinition.Attribute.SINGLETON),
    SITE("site"),
    SIZE(ObjectMemoryResourceDefinition.Attribute.SIZE),
    @Deprecated STACK(JGroupsTransportResourceDefinition.DeprecatedAttribute.STACK),
    @Deprecated START(CacheContainerResourceDefinition.DeprecatedAttribute.START),
    STATISTICS_ENABLED(CacheResourceDefinition.Attribute.STATISTICS_ENABLED),
    STOP_TIMEOUT(TransactionResourceDefinition.Attribute.STOP_TIMEOUT),
    STRATEGY(ObjectMemoryResourceDefinition.DeprecatedAttribute.STRATEGY),
    STRIPING(LockingResourceDefinition.Attribute.STRIPING),
    TAKE_OFFLINE_AFTER_FAILURES(BackupResourceDefinition.TakeOfflineAttribute.AFTER_FAILURES),
    TAKE_OFFLINE_MIN_WAIT(BackupResourceDefinition.TakeOfflineAttribute.MIN_WAIT),
    THREAD_POOL_SIZE(StoreWriteBehindResourceDefinition.Attribute.THREAD_POOL_SIZE),
    TIMEOUT(StateTransferResourceDefinition.Attribute.TIMEOUT),
    TYPE(TableResourceDefinition.ColumnAttribute.ID.getColumnType()),
    @Deprecated VIRTUAL_NODES("virtual-nodes"),

    // hotrod store
    CACHE_CONFIGURATION(HotRodStoreResourceDefinition.Attribute.CACHE_CONFIGURATION),

    // remote-cache-container
    REMOTE_CACHE_CONTAINER(RemoteCacheContainerResourceDefinition.WILDCARD_PATH),
    CONNECTION_TIMEOUT(RemoteCacheContainerResourceDefinition.Attribute.CONNECTION_TIMEOUT),
    DEFAULT_REMOTE_CLUSTER(RemoteCacheContainerResourceDefinition.Attribute.DEFAULT_REMOTE_CLUSTER),
    KEY_SIZE_ESTIMATE(RemoteCacheContainerResourceDefinition.Attribute.KEY_SIZE_ESTIMATE),
    MAX_RETRIES(RemoteCacheContainerResourceDefinition.Attribute.MAX_RETRIES),
    PROTOCOL_VERSION(RemoteCacheContainerResourceDefinition.Attribute.PROTOCOL_VERSION),
    SOCKET_TIMEOUT(RemoteCacheContainerResourceDefinition.Attribute.SOCKET_TIMEOUT),
    TCP_NO_DELAY(RemoteCacheContainerResourceDefinition.Attribute.TCP_NO_DELAY),
    TCP_KEEP_ALIVE(RemoteCacheContainerResourceDefinition.Attribute.TCP_KEEP_ALIVE),
    VALUE_SIZE_ESTIMATE(RemoteCacheContainerResourceDefinition.Attribute.VALUE_SIZE_ESTIMATE),

    // remote-cache-container -> connection-pool
    EXHAUSTED_ACTION(ConnectionPoolResourceDefinition.Attribute.EXHAUSTED_ACTION),
    MAX_ACTIVE(ConnectionPoolResourceDefinition.Attribute.MAX_ACTIVE),
    MAX_WAIT(ConnectionPoolResourceDefinition.Attribute.MAX_WAIT),
    MIN_EVICTABLE_IDLE_TIME(ConnectionPoolResourceDefinition.Attribute.MIN_EVICTABLE_IDLE_TIME),
    MIN_IDLE(ConnectionPoolResourceDefinition.Attribute.MIN_IDLE),

    // remote-cache-container -> remote-clusters
    SOCKET_BINDINGS(RemoteClusterResourceDefinition.Attribute.SOCKET_BINDINGS),

    // remote-cache-container -> security
    SSL_CONTEXT(SecurityResourceDefinition.Attribute.SSL_CONTEXT),
    ;
    private final String name;

    XMLAttribute(Attribute attribute) {
        this(attribute.getDefinition().getXmlName());
    }

    XMLAttribute(PathElement wildcardPath) {
        this(wildcardPath.getKey());
    }

    XMLAttribute(String name) {
        this.name = name;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return this.name;
    }

    private static final Map<String, XMLAttribute> attributes;

    static {
        final Map<String, XMLAttribute> map = new HashMap<>();
        for (XMLAttribute attribute : values()) {
            final String name = attribute.getLocalName();
            if (name != null) {
                assert !map.containsKey(name) : attribute;
                map.put(name, attribute);
            }
        }
        attributes = map;
    }

    public static XMLAttribute forName(String localName) {
        final XMLAttribute attribute = attributes.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
