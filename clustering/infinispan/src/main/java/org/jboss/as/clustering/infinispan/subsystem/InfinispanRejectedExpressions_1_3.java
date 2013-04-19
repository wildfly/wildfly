/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import java.util.Arrays;
import java.util.HashSet;

import org.jboss.as.controller.AttributeDefinition;

/**
 * Class providing lists of attributes which:
 * - accept expressions in 1.4.0 but reject expressions in 1.3.0 (ACCEPT14_REJECT13)
 * - never accept expressions in any version (NEVER)
 * This also notes other edge case subsets.
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanRejectedExpressions_1_3 {

    // attributes which never accept expressions
    public static final AttributeDefinition[] NEVER_CONTAINER_ATTRIBUTES = {
            CacheContainerResourceDefinition.ALIAS,
            CacheContainerResourceDefinition.ALIASES,
            CacheContainerResourceDefinition.EVICTION_EXECUTOR,
            CacheContainerResourceDefinition.LISTENER_EXECUTOR,
            CacheContainerResourceDefinition.NAME,
            CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR
    };

    public static final AttributeDefinition[] NEVER_TRANSPORT_ATTRIBUTES = {
            TransportResourceDefinition.EXECUTOR
    };

    public static final AttributeDefinition[] NEVER_CACHE_ATTRIBUTES = {
            CacheResourceDefinition.NAME
    };

    public static final AttributeDefinition[] NEVER_CHILD_ATTRIBUTES = {
    };

    public static final AttributeDefinition[] NEVER_STORE_ATTRIBUTES = {
            RemoteStoreResourceDefinition.OUTBOUND_SOCKET_BINDING,
            RemoteStoreResourceDefinition.REMOTE_SERVER,
            RemoteStoreResourceDefinition.REMOTE_SERVERS,
            BaseJDBCStoreResourceDefinition.ID_COLUMN,
            BaseJDBCStoreResourceDefinition.DATA_COLUMN,
            BaseJDBCStoreResourceDefinition.TIMESTAMP_COLUMN,
            BaseJDBCStoreResourceDefinition.ENTRY_TABLE,
            BaseJDBCStoreResourceDefinition.BUCKET_TABLE,
            //BaseJDBCStoreResourceDefinition.BINARY_KEYED_TABLE,
            //BaseJDBCStoreResourceDefinition.STRING_KEYED_TABLE
    };

     // attributes which accept in 1.4.0 and in 1.3.0
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES = new AttributeDefinition[]{CacheContainerResourceDefinition.DEFAULT_CACHE} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES = new AttributeDefinition[]{TransportResourceDefinition.CLUSTER} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CACHE_ATTRIBUTES = new AttributeDefinition[] {} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES = new AttributeDefinition[]{TransactionResourceDefinition.MODE} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_STORE_ATTRIBUTES = new AttributeDefinition[]{} ;

    // attributes which need to reject expressions in 1.3
    // set = all - always accept
    public static final AttributeDefinition[] REJECT_CONTAINER_ATTRIBUTES = remove(
            CacheContainerResourceDefinition.CACHE_CONTAINER_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_TRANSPORT_ATTRIBUTES = remove(
            TransportResourceDefinition.TRANSPORT_ATTRIBUTES,
            ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_CACHE_ATTRIBUTES = remove(
            concat(
                CacheResourceDefinition.CACHE_ATTRIBUTES,
                ClusteredCacheResourceDefinition.CLUSTERED_CACHE_ATTRIBUTES,
                DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_ATTRIBUTES,
                new AttributeDefinition[]{DistributedCacheResourceDefinition.VIRTUAL_NODES}
            ),
            ACCEPT14_ACCEPT13_CACHE_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_CHILD_ATTRIBUTES = remove(
            concat(
                    LockingResource.LOCKING_ATTRIBUTES,
                    TransactionResourceDefinition.TRANSACTION_ATTRIBUTES,
                    ExpirationResourceDefinition.EXPIRATION_ATTRIBUTES,
                    EvictionResourceDefinition.EVICTION_ATTRIBUTES,
                    StateTransferResourceDefinition.STATE_TRANSFER_ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_STORE_ATTRIBUTES = remove(
            concat(
                BaseStoreResourceDefinition.COMMON_STORE_ATTRIBUTES,
                StoreResourceDefinition.STORE_ATTRIBUTES,
                FileStoreResourceDefinition.FILE_STORE_ATTRIBUTES,
                RemoteStoreResourceDefinition.REMOTE_STORE_ATTRIBUTES,
                BaseJDBCStoreResourceDefinition.COMMON_BASE_JDBC_STORE_ATTRIBUTES,
                StringKeyedJDBCStoreResourceDefinition.STRING_KEYED_JDBC_STORE_ATTRIBUTES,
                BinaryKeyedJDBCStoreResourceDefinition.BINARY_KEYED_JDBC_STORE_ATTRIBUTES,
                MixedKeyedJDBCStoreResourceDefinition.MIXED_KEYED_JDBC_STORE_ATTRIBUTES,
                StoreWriteBehindResourceDefinition.WRITE_BEHIND_ATTRIBUTES,
                StorePropertyResourceDefinition.STORE_PROPERTY_ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_STORE_ATTRIBUTES
    );

    // attributes which accept in 1.4.0 but reject in 1.3.0 only
    // set = all - never accept - always accept
    public static final AttributeDefinition[] ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES = remove(
            CacheContainerResourceDefinition.CACHE_CONTAINER_ATTRIBUTES,
            NEVER_CONTAINER_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES = remove(
            TransportResourceDefinition.TRANSPORT_ATTRIBUTES,
            NEVER_TRANSPORT_ATTRIBUTES,
            ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_CACHE_ATTRIBUTES = remove(
            concat(
                CacheResourceDefinition.CACHE_ATTRIBUTES,
                ClusteredCacheResourceDefinition.CLUSTERED_CACHE_ATTRIBUTES,
                DistributedCacheResourceDefinition.DISTRIBUTED_CACHE_ATTRIBUTES,
                new AttributeDefinition[]{DistributedCacheResourceDefinition.VIRTUAL_NODES}
            ),
            NEVER_CACHE_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_CHILD_ATTRIBUTES = remove(
            concat(
                    LockingResource.LOCKING_ATTRIBUTES,
                    TransactionResourceDefinition.TRANSACTION_ATTRIBUTES,
                    ExpirationResourceDefinition.EXPIRATION_ATTRIBUTES,
                    EvictionResourceDefinition.EVICTION_ATTRIBUTES,
                    StateTransferResourceDefinition.STATE_TRANSFER_ATTRIBUTES
            ),
            NEVER_CHILD_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_STORE_ATTRIBUTES = remove(
            concat(
                BaseStoreResourceDefinition.COMMON_STORE_ATTRIBUTES,
                StoreResourceDefinition.STORE_ATTRIBUTES,
                FileStoreResourceDefinition.FILE_STORE_ATTRIBUTES,
                RemoteStoreResourceDefinition.REMOTE_STORE_ATTRIBUTES,
                BaseJDBCStoreResourceDefinition.COMMON_BASE_JDBC_STORE_ATTRIBUTES,
                StringKeyedJDBCStoreResourceDefinition.STRING_KEYED_JDBC_STORE_ATTRIBUTES,
                BinaryKeyedJDBCStoreResourceDefinition.BINARY_KEYED_JDBC_STORE_ATTRIBUTES,
                MixedKeyedJDBCStoreResourceDefinition.MIXED_KEYED_JDBC_STORE_ATTRIBUTES,
                StoreWriteBehindResourceDefinition.WRITE_BEHIND_ATTRIBUTES,
                StorePropertyResourceDefinition.STORE_PROPERTY_ATTRIBUTES

             ),
             NEVER_STORE_ATTRIBUTES
    );

    /**
     * Helper methods to create arrays of attributes which need to have transformers applied.
     */
    private static AttributeDefinition[] concat(AttributeDefinition[]... additions) {
        HashSet<AttributeDefinition> result = new HashSet<AttributeDefinition>();
        for (int i = 0; i < additions.length; i++)
            result.addAll(Arrays.asList(additions[i]));
        return result.toArray(new AttributeDefinition[0]);
    }

    private static AttributeDefinition[] remove(AttributeDefinition[] initial, AttributeDefinition[]... removals) {
        HashSet<AttributeDefinition> result = new HashSet<AttributeDefinition>(Arrays.asList(initial));
        for (int i = 0; i < removals.length; i++)
            result.removeAll(Arrays.asList(removals[i]));
        return result.toArray(new AttributeDefinition[0]);
    }
}
