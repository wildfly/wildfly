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
            CacheContainerResourceDefinition.ALIASES,
            CacheContainerResourceDefinition.EVICTION_EXECUTOR,
            CacheContainerResourceDefinition.LISTENER_EXECUTOR,
            CacheContainerResourceDefinition.REPLICATION_QUEUE_EXECUTOR
    };

    public static final AttributeDefinition[] NEVER_TRANSPORT_ATTRIBUTES = {
            TransportResourceDefinition.EXECUTOR
    };

    public static final AttributeDefinition[] NEVER_CHILD_ATTRIBUTES = {
    };

    public static final AttributeDefinition[] NEVER_STORE_ATTRIBUTES = {
            RemoteStoreResourceDefinition.OUTBOUND_SOCKET_BINDING,
            RemoteStoreResourceDefinition.REMOTE_SERVER,
            RemoteStoreResourceDefinition.REMOTE_SERVERS,
            JDBCStoreResourceDefinition.ID_COLUMN,
            JDBCStoreResourceDefinition.DATA_COLUMN,
            JDBCStoreResourceDefinition.TIMESTAMP_COLUMN,
            JDBCStoreResourceDefinition.ENTRY_TABLE,
            JDBCStoreResourceDefinition.BUCKET_TABLE,
    };

     // attributes which accept in 1.4.0 and in 1.3.0
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES = new AttributeDefinition[] { CacheContainerResourceDefinition.DEFAULT_CACHE };
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES = new AttributeDefinition[] { TransportResourceDefinition.CLUSTER };
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CACHE_ATTRIBUTES = new AttributeDefinition[0];
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES = new AttributeDefinition[]{ TransactionResourceDefinition.MODE };
    public static final AttributeDefinition[] ACCEPT14_ACCEPT13_STORE_ATTRIBUTES = new AttributeDefinition[0];

    // attributes which need to reject expressions in 1.3
    // set = all - always accept
    public static final AttributeDefinition[] REJECT_CONTAINER_ATTRIBUTES = remove(
            CacheContainerResourceDefinition.ATTRIBUTES,
            ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_TRANSPORT_ATTRIBUTES = remove(
            TransportResourceDefinition.ATTRIBUTES,
            ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES
    );

    @SuppressWarnings("deprecation")
    public static final AttributeDefinition[] REJECT_CACHE_ATTRIBUTES = remove(
            concat(
                CacheResourceDefinition.ATTRIBUTES,
                ClusteredCacheResourceDefinition.ATTRIBUTES,
                DistributedCacheResourceDefinition.ATTRIBUTES,
                new AttributeDefinition[] { DistributedCacheResourceDefinition.VIRTUAL_NODES }
            ),
            ACCEPT14_ACCEPT13_CACHE_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_CHILD_ATTRIBUTES = remove(
            concat(
                    LockingResourceDefinition.ATTRIBUTES,
                    TransactionResourceDefinition.ATTRIBUTES,
                    ExpirationResourceDefinition.ATTRIBUTES,
                    EvictionResourceDefinition.ATTRIBUTES,
                    StateTransferResourceDefinition.ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES
    );

    public static final AttributeDefinition[] REJECT_STORE_ATTRIBUTES = remove(
            concat(
                StoreResourceDefinition.ATTRIBUTES,
                CustomStoreResourceDefinition.ATTRIBUTES,
                FileStoreResourceDefinition.ATTRIBUTES,
                RemoteStoreResourceDefinition.ATTRIBUTES,
                JDBCStoreResourceDefinition.ATTRIBUTES,
                JDBCStoreResourceDefinition.COLUMN_ATTRIBUTES,
                JDBCStoreResourceDefinition.TABLE_ATTRIBUTES,
                new AttributeDefinition[] { JDBCStoreResourceDefinition.ENTRY_TABLE, JDBCStoreResourceDefinition.BUCKET_TABLE, JDBCStoreResourceDefinition.STRING_KEYED_TABLE, JDBCStoreResourceDefinition.BINARY_KEYED_TABLE },
                StringKeyedJDBCStoreResourceDefinition.ATTRIBUTES,
                BinaryKeyedJDBCStoreResourceDefinition.ATTRIBUTES,
                MixedKeyedJDBCStoreResourceDefinition.ATTRIBUTES,
                StoreWriteBehindResourceDefinition.ATTRIBUTES,
                StorePropertyResourceDefinition.ATTRIBUTES
            ),
            ACCEPT14_ACCEPT13_STORE_ATTRIBUTES
    );

    // attributes which accept in 1.4.0 but reject in 1.3.0 only
    // set = all - never accept - always accept
    public static final AttributeDefinition[] ACCEPT14_REJECT13_CONTAINER_ATTRIBUTES = remove(
            CacheContainerResourceDefinition.ATTRIBUTES,
            NEVER_CONTAINER_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CONTAINER_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_TRANSPORT_ATTRIBUTES = remove(
            TransportResourceDefinition.ATTRIBUTES,
            NEVER_TRANSPORT_ATTRIBUTES,
            ACCEPT14_ACCEPT13_TRANSPORT_ATTRIBUTES
    );

    @SuppressWarnings("deprecation")
    public static final AttributeDefinition[] ACCEPT14_REJECT13_CACHE_ATTRIBUTES = concat(
            CacheResourceDefinition.ATTRIBUTES,
            ClusteredCacheResourceDefinition.ATTRIBUTES,
            DistributedCacheResourceDefinition.ATTRIBUTES,
            new AttributeDefinition[] { DistributedCacheResourceDefinition.VIRTUAL_NODES }
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_CHILD_ATTRIBUTES = remove(
            concat(
                    LockingResourceDefinition.ATTRIBUTES,
                    TransactionResourceDefinition.ATTRIBUTES,
                    ExpirationResourceDefinition.ATTRIBUTES,
                    EvictionResourceDefinition.ATTRIBUTES,
                    StateTransferResourceDefinition.ATTRIBUTES
            ),
            NEVER_CHILD_ATTRIBUTES,
            ACCEPT14_ACCEPT13_CHILD_ATTRIBUTES
    );

    public static final AttributeDefinition[] ACCEPT14_REJECT13_STORE_ATTRIBUTES = remove(
            concat(
                StoreResourceDefinition.ATTRIBUTES,
                CustomStoreResourceDefinition.ATTRIBUTES,
                FileStoreResourceDefinition.ATTRIBUTES,
                RemoteStoreResourceDefinition.ATTRIBUTES,
                JDBCStoreResourceDefinition.ATTRIBUTES,
                JDBCStoreResourceDefinition.COLUMN_ATTRIBUTES,
                JDBCStoreResourceDefinition.TABLE_ATTRIBUTES,
                new AttributeDefinition[] { JDBCStoreResourceDefinition.ENTRY_TABLE, JDBCStoreResourceDefinition.BUCKET_TABLE, JDBCStoreResourceDefinition.STRING_KEYED_TABLE, JDBCStoreResourceDefinition.BINARY_KEYED_TABLE },
                StringKeyedJDBCStoreResourceDefinition.ATTRIBUTES,
                BinaryKeyedJDBCStoreResourceDefinition.ATTRIBUTES,
                MixedKeyedJDBCStoreResourceDefinition.ATTRIBUTES,
                StoreWriteBehindResourceDefinition.ATTRIBUTES,
                StorePropertyResourceDefinition.ATTRIBUTES

             ),
             NEVER_STORE_ATTRIBUTES
    );

    /**
     * Helper methods to create arrays of attributes which need to have transformers applied.
     */
    private static AttributeDefinition[] concat(AttributeDefinition[]... additions) {
        HashSet<AttributeDefinition> result = new HashSet<>();
        for (int i = 0; i < additions.length; i++)
            result.addAll(Arrays.asList(additions[i]));
        return result.toArray(new AttributeDefinition[0]);
    }

    private static AttributeDefinition[] remove(AttributeDefinition[] initial, AttributeDefinition[]... removals) {
        HashSet<AttributeDefinition> result = new HashSet<>(Arrays.asList(initial));
        for (int i = 0; i < removals.length; i++)
            result.removeAll(Arrays.asList(removals[i]));
        return result.toArray(new AttributeDefinition[0]);
    }
}
