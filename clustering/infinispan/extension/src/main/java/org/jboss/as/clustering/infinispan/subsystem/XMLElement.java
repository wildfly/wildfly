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
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteClusterResourceDefinition;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;

/**
 * Enumerates the elements used in the Infinispan subsystem schema.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 RedHat Inc.
 * @author Tristan Tarrant
 */
public enum XMLElement {
    // must be first
    UNKNOWN(""),

    ALIAS("alias"),
    ASYNC_OPERATIONS_THREAD_POOL("async-operations-thread-pool"),
    BACKUP(BackupResourceDefinition.WILDCARD_PATH),
    @Deprecated BACKUP_FOR(BackupForResourceDefinition.PATH),
    BACKUPS(BackupsResourceDefinition.PATH),
    BINARY_KEYED_TABLE("binary-keyed-table"),
    @Deprecated BUCKET_TABLE("bucket-table"),
    CACHE_CONTAINER(CacheContainerResourceDefinition.WILDCARD_PATH),
    DATA_COLUMN(TableResourceDefinition.ColumnAttribute.DATA),
    DISTRIBUTED_CACHE(DistributedCacheResourceDefinition.WILDCARD_PATH),
    @Deprecated ENTRY_TABLE("entry-table"),
    @Deprecated EVICTION(ObjectMemoryResourceDefinition.EVICTION_PATH),
    BINARY_MEMORY("binary-memory"),
    OBJECT_MEMORY("object-memory"),
    OFF_HEAP_MEMORY("off-heap-memory"),
    EXPIRATION(ExpirationResourceDefinition.PATH),
    EXPIRATION_THREAD_POOL("expiration-thread-pool"),
    FILE_STORE("file-store"),
    ID_COLUMN(TableResourceDefinition.ColumnAttribute.ID),
    INVALIDATION_CACHE(InvalidationCacheResourceDefinition.WILDCARD_PATH),
    LISTENER_THREAD_POOL("listener-thread-pool"),
    JDBC_STORE("jdbc-store"),
    STRING_KEYED_JDBC_STORE("string-keyed-jdbc-store"),
    BINARY_KEYED_JDBC_STORE("binary-keyed-jdbc-store"),
    MIXED_KEYED_JDBC_STORE("mixed-keyed-jdbc-store"),
    @Deprecated INDEXING(CacheResourceDefinition.DeprecatedAttribute.INDEXING),
    LOCAL_CACHE(LocalCacheResourceDefinition.WILDCARD_PATH),
    LOCKING(LockingResourceDefinition.PATH),
    PARTITION_HANDLING(PartitionHandlingResourceDefinition.PATH),
    PERSISTENCE_THREAD_POOL("persistence-thread-pool"),
    REMOTE_COMMAND_THREAD_POOL("remote-command-thread-pool"),
    PROPERTY(ModelDescriptionConstants.PROPERTY),
    @Deprecated REHASHING("rehashing"),
    REMOTE_SERVER("remote-server"),
    REMOTE_STORE("remote-store"),
    REPLICATED_CACHE(ReplicatedCacheResourceDefinition.WILDCARD_PATH),
    SCATTERED_CACHE(ScatteredCacheResourceDefinition.WILDCARD_PATH),
    STATE_TRANSFER(StateTransferResourceDefinition.PATH),
    STATE_TRANSFER_THREAD_POOL("state-transfer-thread-pool"),
    STORE(StoreResourceDefinition.WILDCARD_PATH),
    STRING_KEYED_TABLE("string-keyed-table"),
    TABLE(TableResourceDefinition.WILDCARD_PATH),
    TAKE_OFFLINE("take-offline"),
    TIMESTAMP_COLUMN(TableResourceDefinition.ColumnAttribute.TIMESTAMP),
    TRANSACTION(TransactionResourceDefinition.PATH),
    TRANSPORT(TransportResourceDefinition.WILDCARD_PATH),
    TRANSPORT_THREAD_POOL("transport-thread-pool"),
    WRITE_BEHIND("write-behind"),

    // remote-cache-container
    REMOTE_CACHE_CONTAINER(RemoteCacheContainerResourceDefinition.WILDCARD_PATH),
    ASYNC_THREAD_POOL("async-thread-pool"),
    CONNECTION_POOL(ConnectionPoolResourceDefinition.PATH),
    INVALIDATION_NEAR_CACHE("invalidation-near-cache"),
    REMOTE_CLUSTERS("remote-clusters"),
    REMOTE_CLUSTER(RemoteClusterResourceDefinition.WILDCARD_PATH),
    SECURITY("security"),
    HOTROD_STORE("hotrod-store"),
    ;

    private final String name;

    XMLElement(Attribute attribute) {
        this(attribute.getDefinition().getXmlName());
    }

    XMLElement(PathElement path) {
        this.name = path.isWildcard() ? path.getKey() : path.getValue();
    }

    XMLElement(String name) {
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

    private static final Map<String, XMLElement> elements;

    static {
        final Map<String, XMLElement> map = new HashMap<>();
        for (XMLElement element : values()) {
            final String name = element.getLocalName();
            if (name != null) {
                assert !map.containsKey(name) : element;
                map.put(name, element);
            }
        }
        elements = map;
    }

    public static XMLElement forName(String localName) {
        final XMLElement element = elements.get(localName);
        return element == null ? UNKNOWN : element;
    }
}
