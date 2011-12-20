package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Locale;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanSubsystemProviders {

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getSubsystemDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getSubsystemAddDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getSubsystemRemoveDescription(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getSubsystemDescribeDescription(locale);
        }
    };

    static final DescriptionProvider CACHE_CONTAINER = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheContainerDescription(locale);
        }
    };

    static final DescriptionProvider CACHE_CONTAINER_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheContainerAddDescription(locale);
        }
    };

    static final DescriptionProvider CACHE_CONTAINER_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheContainerRemoveDescription(locale);
        }
    };

    static final DescriptionProvider REMOVE_ALIAS = new DescriptionProvider() {
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getRemoveAliasCommandDescription(locale);
    }
    };

    static final DescriptionProvider ADD_ALIAS = new DescriptionProvider() {
    public ModelNode getModelDescription(Locale locale) {
        return InfinispanDescriptions.getAddAliasCommandDescription(locale);
    }
    };

    static final DescriptionProvider LOCAL_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getLocalCacheDescription(locale);
        }
    };

    static final DescriptionProvider LOCAL_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getLocalCacheAddDescription(locale);
        }
    };

    static final DescriptionProvider INVALIDATION_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getInvalidationCacheDescription(locale);
        }
    };

    static final DescriptionProvider INVALIDATION_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getInvalidationCacheAddDescription(locale);
        }
    };

    static final DescriptionProvider REPLICATED_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getReplicatedCacheDescription(locale);
        }
    };

    static final DescriptionProvider REPLICATED_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getReplicatedCacheAddDescription(locale);
        }
    };

    static final DescriptionProvider DISTRIBUTED_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getDistributedCacheDescription(locale);
        }
    };

    static final DescriptionProvider DISTRIBUTED_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getDistributedCacheAddDescription(locale);
        }
    };

    static final DescriptionProvider CACHE_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheRemoveDescription(locale);
        }
    };

    static final DescriptionProvider TRANSPORT = new DescriptionProvider() {
         public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getTransportDescription(locale);
        }
    };

    static final DescriptionProvider TRANSPORT_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getTransportAddDescription(locale);
        }
    };

    static final DescriptionProvider TRANSPORT_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getTransportRemoveDescription(locale);
        }
    };

    static final DescriptionProvider LOCKING = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getLockingDescription(locale);
        }
    };
    static final DescriptionProvider LOCKING_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getLockingAddDescription(locale);
        }
    };
    static final DescriptionProvider LOCKING_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getLockingRemoveDescription(locale);
        }
    };

    static final DescriptionProvider TRANSACTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getTransactionDescription(locale);
        }
    };
    static final DescriptionProvider TRANSACTION_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getTransactionAddDescription(locale);
        }
    };
    static final DescriptionProvider TRANSACTION_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getTransactionRemoveDescription(locale);
        }
    };

    static final DescriptionProvider EVICTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getEvictionDescription(locale);
        }
    };
    static final DescriptionProvider EVICTION_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getEvictionAddDescription(locale);
        }
    };
    static final DescriptionProvider EVICTION_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getEvictionRemoveDescription(locale);
        }
    };

    static final DescriptionProvider EXPIRATION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getExpirationDescription(locale);
        }
    };
    static final DescriptionProvider EXPIRATION_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getExpirationAddDescription(locale);
        }
    };
    static final DescriptionProvider EXPIRATION_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getExpirationRemoveDescription(locale);
        }
    };

    static final DescriptionProvider STATE_TRANSFER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getStateTransferDescription(locale);
        }
    };
    static final DescriptionProvider STATE_TRANSFER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getStateTransferAddDescription(locale);
        }
    };
    static final DescriptionProvider STATE_TRANSFER_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getStateTransferRemoveDescription(locale);
        }
    };

    static final DescriptionProvider REHASHING = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getRehashingDescription(locale);
        }
    };
    static final DescriptionProvider REHASHING_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getRehashingAddDescription(locale);
        }
    };
    static final DescriptionProvider REHASHING_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getRehashingRemoveDescription(locale);
        }
    };



    public static final DescriptionProvider STORE_PROPERTY = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheStorePropertyDescription(locale);
        }
    };
    public static final DescriptionProvider STORE_PROPERTY_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheStorePropertyAddDescription(locale);
        }
    };
    public static final DescriptionProvider STORE_PROPERTY_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            return InfinispanDescriptions.getCacheStorePropertyRemoveDescription(locale);
        }
    };


}
