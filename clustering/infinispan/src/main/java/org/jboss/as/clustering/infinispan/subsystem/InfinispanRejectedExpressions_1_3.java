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
            CacheContainerResource.ALIAS,
            CacheContainerResource.ALIASES,
            CacheContainerResource.EVICTION_EXECUTOR,
            CacheContainerResource.LISTENER_EXECUTOR,
            CacheContainerResource.NAME,
            CacheContainerResource.REPLICATION_QUEUE_EXECUTOR
    };

    public static final AttributeDefinition[] NEVER_TRANSPORT_ATTRIBUTES = {
            TransportResource.EXECUTOR
    };

    public static final AttributeDefinition[] NEVER_CACHE_ATTRIBUTES = {
            CacheResource.NAME
    };

    public static final AttributeDefinition[] NEVER_CHILD_ATTRIBUTES = {
    };

    public static final AttributeDefinition[] NEVER_STORE_ATTRIBUTES = {
            RemoteStoreResource.OUTBOUND_SOCKET_BINDING,
            RemoteStoreResource.REMOTE_SERVER,
            RemoteStoreResource.REMOTE_SERVERS,
            BaseJDBCStoreResource.ID_COLUMN,
            BaseJDBCStoreResource.DATA_COLUMN,
            BaseJDBCStoreResource.TIMESTAMP_COLUMN,
            BaseJDBCStoreResource.ENTRY_TABLE,
            BaseJDBCStoreResource.BUCKET_TABLE,
            BaseJDBCStoreResource.BINARY_KEYED_TABLE,
            BaseJDBCStoreResource.STRING_KEYED_TABLE
    };

     // attributes which accept in 1.4.0 and in 1.3.0
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES = new AttributeDefinition[]{CacheContainerResource.DEFAULT_CACHE} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES = new AttributeDefinition[]{TransportResource.CLUSTER} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CACHE_ATTRIBUTES = new AttributeDefinition[] {} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES = new AttributeDefinition[]{TransactionResource.MODE} ;
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_STORE_ATTRIBUTES = new AttributeDefinition[]{} ;

    // attributes which need to reject expressions in 1.3
    // set = all - always accept
    public static final AttributeDefinition[] REJECT_CONTAINER_ATTRIBUTES = remove(
            CacheContainerResource.CACHE_CONTAINER_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_TRANSPORT_ATTRIBUTES = remove(
            TransportResource.TRANSPORT_ATTRIBUTES,
            ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_CACHE_ATTRIBUTES = remove(
            concat(
                CacheResource.CACHE_ATTRIBUTES,
                ClusteredCacheResource.CLUSTERED_CACHE_ATTRIBUTES,
                DistributedCacheResource.DISTRIBUTED_CACHE_ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_CACHE_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_CHILD_ATTRIBUTES = remove(
            concat(
                    LockingResource.LOCKING_ATTRIBUTES,
                    TransactionResource.TRANSACTION_ATTRIBUTES,
                    ExpirationResource.EXPIRATION_ATTRIBUTES,
                    EvictionResource.EVICTION_ATTRIBUTES,
                    StateTransferResource.STATE_TRANSFER_ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_STORE_ATTRIBUTES = remove(
            concat(
                BaseStoreResource.COMMON_STORE_ATTRIBUTES,
                StoreResource.STORE_ATTRIBUTES,
                FileStoreResource.FILE_STORE_ATTRIBUTES,
                RemoteStoreResource.REMOTE_STORE_ATTRIBUTES,
                BaseJDBCStoreResource.COMMON_BASE_JDBC_STORE_ATTRIBUTES,
                StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_ATTRIBUTES,
                BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_ATTRIBUTES,
                MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_ATTRIBUTES,
                StoreWriteBehindResource.WRITE_BEHIND_ATTRIBUTES,
                StorePropertyResource.STORE_PROPERTY_ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_STORE_ATTRIBUTES
    );


    // attributes which accept in 1.4.0 but reject in 1.3.0 only
    // set = all - never accept - always accept
    public static final AttributeDefinition[] ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES = remove(
            CacheContainerResource.CACHE_CONTAINER_ATTRIBUTES,
            NEVER_CONTAINER_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES = remove(
            TransportResource.TRANSPORT_ATTRIBUTES,
            NEVER_TRANSPORT_ATTRIBUTES,
            ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_CACHE_ATTRIBUTES = remove(
            concat(
                CacheResource.CACHE_ATTRIBUTES,
                ClusteredCacheResource.CLUSTERED_CACHE_ATTRIBUTES,
                DistributedCacheResource.DISTRIBUTED_CACHE_ATTRIBUTES
            ),
            NEVER_CACHE_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_CHILD_ATTRIBUTES = remove(
            concat(
                    LockingResource.LOCKING_ATTRIBUTES,
                    TransactionResource.TRANSACTION_ATTRIBUTES,
                    ExpirationResource.EXPIRATION_ATTRIBUTES,
                    EvictionResource.EVICTION_ATTRIBUTES,
                    StateTransferResource.STATE_TRANSFER_ATTRIBUTES
            ),
            NEVER_CHILD_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_STORE_ATTRIBUTES = remove(
            concat(
                BaseStoreResource.COMMON_STORE_ATTRIBUTES,
                StoreResource.STORE_ATTRIBUTES,
                FileStoreResource.FILE_STORE_ATTRIBUTES,
                RemoteStoreResource.REMOTE_STORE_ATTRIBUTES,
                BaseJDBCStoreResource.COMMON_BASE_JDBC_STORE_ATTRIBUTES,
                StringKeyedJDBCStoreResource.STRING_KEYED_JDBC_STORE_ATTRIBUTES,
                BinaryKeyedJDBCStoreResource.BINARY_KEYED_JDBC_STORE_ATTRIBUTES,
                MixedKeyedJDBCStoreResource.MIXED_KEYED_JDBC_STORE_ATTRIBUTES,
                StoreWriteBehindResource.WRITE_BEHIND_ATTRIBUTES,
                StorePropertyResource.STORE_PROPERTY_ATTRIBUTES
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
