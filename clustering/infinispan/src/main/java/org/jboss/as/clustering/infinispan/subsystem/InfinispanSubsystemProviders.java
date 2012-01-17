package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.DATA_SOURCE;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.PATH;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.ALIAS;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.BATCHING;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.DEFAULT_CACHE_CONTAINER;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.INDEXING;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.NAME;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.PROPERTY;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.REMOTE_SERVER;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.START;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.VALUE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class InfinispanSubsystemProviders {

    public static final String RESOURCE_NAME = InfinispanSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(resources.getString("infinispan"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUri());
            subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, DESCRIPTION).set(resources.getString("infinispan.container"));
            subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MIN_OCCURS).set(1);
            subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MAX_OCCURS).set(Integer.MAX_VALUE);
            subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MODEL_DESCRIPTION).setEmptyObject();
            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(resources.getString("infinispan.add"));
            DEFAULT_CACHE_CONTAINER.addOperationParameterDescription(resources, "infinispan", op);
            return op;
        }
    };

    static final DescriptionProvider SUBSYSTEM_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            final ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(resources.getString("infinispan.remove"));
            op.get(REPLY_PROPERTIES).setEmptyObject();
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider SUBSYSTEM_DESCRIBE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(resources.getString("infinispan.describe"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            op.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
            op.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
            return op;
        }
    };

    static final DescriptionProvider CACHE_CONTAINER = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode container = new ModelNode();
            String keyPrefix = "infinispan.container" ;
            container.get(DESCRIPTION).set(resources.getString(keyPrefix));
            // attributes
            for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, keyPrefix, container);
            }
            // need to add value type until we replace with a ListAttribute
            ALIAS.addResourceAttributeDescription(resources, keyPrefix, container).
                    get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
            // information about its child "singleton=transport"
            container.get(CHILDREN, ModelKeys.SINGLETON, DESCRIPTION).set(resources.getString(keyPrefix + ".singleton"));
            container.get(CHILDREN, ModelKeys.SINGLETON, MIN_OCCURS).set(0);
            container.get(CHILDREN, ModelKeys.SINGLETON, MAX_OCCURS).set(1);
            container.get(CHILDREN, ModelKeys.SINGLETON, ALLOWED).setEmptyList().add("transport");
            container.get(CHILDREN, ModelKeys.SINGLETON, MODEL_DESCRIPTION);
            // information about its child "local-cache"
            container.get(CHILDREN, ModelKeys.LOCAL_CACHE, DESCRIPTION).set(resources.getString(keyPrefix + ".local-cache"));
            container.get(CHILDREN, ModelKeys.LOCAL_CACHE, MIN_OCCURS).set(0);
            container.get(CHILDREN, ModelKeys.LOCAL_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
            container.get(CHILDREN, ModelKeys.LOCAL_CACHE, MODEL_DESCRIPTION);
            // information about its child "invalidation-cache"
            container.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, DESCRIPTION).set(resources.getString(keyPrefix + ".invalidation-cache"));
            container.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, MIN_OCCURS).set(0);
            container.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
            container.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, MODEL_DESCRIPTION);
            // information about its child "local-cache"
            container.get(CHILDREN, ModelKeys.REPLICATED_CACHE, DESCRIPTION).set(resources.getString(keyPrefix + ".replicated-cache"));
            container.get(CHILDREN, ModelKeys.REPLICATED_CACHE, MIN_OCCURS).set(0);
            container.get(CHILDREN, ModelKeys.REPLICATED_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
            container.get(CHILDREN, ModelKeys.REPLICATED_CACHE, MODEL_DESCRIPTION);
            // information about its child "local-cache"
            container.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, DESCRIPTION).set(resources.getString(keyPrefix + ".distributed-cache"));
            container.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, MIN_OCCURS).set(0);
            container.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
            container.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, MODEL_DESCRIPTION);

            return container;
        }
    };

    static final DescriptionProvider CACHE_CONTAINER_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(ADD);
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.add"));
            // request parameters
            for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.container", op);
            }
            // need to add value type until we replace with a ListAttribute
            ALIAS.addOperationParameterDescription(resources, "infinispan.container", op).
                    get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
            return op;
        }
    };

    static final DescriptionProvider CACHE_CONTAINER_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider ADD_ALIAS = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add-alias");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.alias.add-alias"));
            NAME.addOperationParameterDescription(resources, "infinispan.container.alias", op);
            return op;
        }
    };

    static final DescriptionProvider REMOVE_ALIAS = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove-alias");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.alias.remove-alias"));
            NAME.addOperationParameterDescription(resources, "infinispan.container.alias", op);
            return op;
        }
    };


    static final DescriptionProvider LOCAL_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode cache = new ModelNode();
            cache.get(DESCRIPTION).set(resources.getString("infinispan.container.local-cache"));
            // attributes
            for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
            }
            // children
            addCommonCacheChildren("infinispan.cache", cache, resources);
            return cache ;
        }
    };

    static final DescriptionProvider LOCAL_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.local-cache.add"));

            addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
            return op;
        }
    };

    static final DescriptionProvider INVALIDATION_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode cache = new ModelNode();
            cache.get(DESCRIPTION).set(resources.getString("infinispan.container.invalidation-cache"));
            // attributes
            for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
            }
            // children
            addCommonCacheChildren("infinispan.cache", cache, resources);
            addStateTransferCacheChildren("infinispan-cache", cache, resources);
            return cache ;
        }
    };

    static final DescriptionProvider INVALIDATION_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.invalidation-cache.add"));
            // parameters
            addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
            addCommonClusteredCacheAddRequestProperties("infinispan.clustered-cache", op, resources);
            // nested resource initialization
            String keyPrefix = "infinispan.replicated-cache.state-transfer" ;
            ModelNode requestProperties = op.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
            ModelNode stateTransfer = addNode(requestProperties, ModelKeys.STATE_TRANSFER, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
            for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
                addAttributeDescription(attr, resources, keyPrefix, stateTransfer);
            }
            return op;
        }
    };

    static final DescriptionProvider REPLICATED_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode cache = new ModelNode();
            cache.get(DESCRIPTION).set(resources.getString("infinispan.container.replicated-cache"));
            // attributes
            for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
            }
            // children
            addCommonCacheChildren("infinispan.cache", cache, resources);
            addStateTransferCacheChildren("infinispan-cache", cache, resources);
            return cache ;
        }
    };

    static final DescriptionProvider REPLICATED_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.replicated-cache.add"));
            // parameters
            addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
            addCommonClusteredCacheAddRequestProperties("infinispan.clustered-cache", op, resources);
            // nested resource initialization
            String keyPrefix = "infinispan.replicated-cache.state-transfer" ;
            ModelNode requestProperties = op.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
            ModelNode stateTransfer = addNode(requestProperties, ModelKeys.STATE_TRANSFER, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
            for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
                addAttributeDescription(attr, resources, keyPrefix, stateTransfer);
            }
            return op;
        }
    };

    static final DescriptionProvider DISTRIBUTED_CACHE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode cache = new ModelNode();
            cache.get(DESCRIPTION).set(resources.getString("infinispan.container.distributed-cache"));
            // attributes
            for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
            }
            // children
            addCommonCacheChildren("infinispan.cache", cache, resources);
            addRehashingCacheChildren("infinispan-cache", cache, resources);
            return cache ;
        }
    };

    static final DescriptionProvider DISTRIBUTED_CACHE_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.distributed-cache.add"));
            // parameters
            addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
            addCommonClusteredCacheAddRequestProperties("infinispan.clustered-cache", op, resources);
            for (AttributeDefinition attr : CommonAttributes.DISTRIBUTED_CACHE_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.distributed-cache", op);
            }
            // nested resource initialization
            String keyPrefix = "infinispan.distributed-cache.rehashing" ;
            ModelNode requestProperties = op.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
            ModelNode rehashing = addNode(requestProperties, ModelKeys.REHASHING, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
            for (AttributeDefinition attr : CommonAttributes.REHASHING_ATTRIBUTES) {
                addAttributeDescription(attr, resources, keyPrefix, rehashing);
            }
            return op;
        }
    };

    static final DescriptionProvider CACHE_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set(REMOVE);
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
         }
    };

    static final DescriptionProvider TRANSPORT = new DescriptionProvider() {
         public ModelNode getModelDescription(Locale locale) {
             ResourceBundle resources = getResources(locale);
             final ModelNode transport = new ModelNode();
             transport.get(DESCRIPTION).set(resources.getString("infinispan.container.transport"));
             for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
                 attr.addResourceAttributeDescription(resources, "infinispan.container.transport", transport);
             }
             return transport ;
        }
    };

    static final DescriptionProvider TRANSPORT_ADD = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.transport.add"));
            for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.container.transport", op);
            }
            return op;
        }
    };

    static final DescriptionProvider TRANSPORT_REMOVE = new DescriptionProvider() {
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.container.transport.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider LOCKING = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode locking = new ModelNode();
            locking.get(DESCRIPTION).set(resources.getString("infinispan.cache.locking"));
            for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache.locking", locking);
            }
            return locking ;
        }
    };
    static final DescriptionProvider LOCKING_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.locking.add"));
            for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.cache.locking", op);
            }
            return op;
        }
    };
    static final DescriptionProvider LOCKING_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.locking.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider TRANSACTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode transaction = new ModelNode();
            transaction.get(DESCRIPTION).set(resources.getString("infinispan.cache.transaction"));
            for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache.transaction", transaction);
            }
            return transaction ;
        }
    };
    static final DescriptionProvider TRANSACTION_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.transaction.add"));
            for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.cache.transaction", op);
            }
            return op;
        }
    };
    static final DescriptionProvider TRANSACTION_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.transaction.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider EVICTION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode eviction = new ModelNode();
            eviction.get(DESCRIPTION).set(resources.getString("infinispan.cache.eviction"));
            for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache.eviction", eviction);
            }
            return eviction ;
        }
    };
    static final DescriptionProvider EVICTION_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.eviction.add"));
            for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.cache.eviction", op);
            }
            return op;
        }
    };
    static final DescriptionProvider EVICTION_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.eviction.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider EXPIRATION = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode expiration = new ModelNode();
            expiration.get(DESCRIPTION).set(resources.getString("infinispan.cache.expiration"));
            for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.cache.expiration", expiration);
            }
            return expiration ;
        }
    };
    static final DescriptionProvider EXPIRATION_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.expiration.add"));
            for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.cache.expiration", op);
            }
            return op;
        }
    };
    static final DescriptionProvider EXPIRATION_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.expiration.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider STATE_TRANSFER = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode stateTransfer = new ModelNode();
            stateTransfer.get(DESCRIPTION).set(resources.getString("infinispan.replicated-cache.state-transfer"));
            for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.replicated-cache.state-transfer", stateTransfer);
            }
            return stateTransfer ;
        }
    };
    static final DescriptionProvider STATE_TRANSFER_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.replicated-cache.state-transfer.add"));
            for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.replicated-cache.state-transfer", op);
            }
            return op;
        }
    };
    static final DescriptionProvider STATE_TRANSFER_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.replicated-cache.state-transfer.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider REHASHING = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode rehashing = new ModelNode();
            rehashing.get(DESCRIPTION).set(resources.getString("infinispan.distributed-cache.rehashing"));
            for (AttributeDefinition attr : CommonAttributes.REHASHING_ATTRIBUTES) {
                attr.addResourceAttributeDescription(resources, "infinispan.distributed-cache.rehashing", rehashing);
            }
            return rehashing ;
        }
    };
    static final DescriptionProvider REHASHING_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.distributed-cache.rehashing.add"));
            for (AttributeDefinition attr : CommonAttributes.REHASHING_ATTRIBUTES) {
                attr.addOperationParameterDescription(resources, "infinispan.distributed-cache.rehashing", op);
            }
            return op;
        }
    };
    static final DescriptionProvider REHASHING_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.distributed-cache.rehashing.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };

    static final DescriptionProvider STORE_PROPERTY = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode storeProperty = new ModelNode();
            storeProperty.get(DESCRIPTION).set(resources.getString("infinispan.cache.store.property"));
            VALUE.addResourceAttributeDescription(resources, "infinispan.cache.store.property", storeProperty);
            return storeProperty ;
        }
    };
    static final DescriptionProvider STORE_PROPERTY_ADD = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("add");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.store.property.add"));
            VALUE.addOperationParameterDescription(resources, "infinispan.cache.store.property", op);
            return op;
        }
    };
    static final DescriptionProvider STORE_PROPERTY_REMOVE = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(Locale locale) {
            ResourceBundle resources = getResources(locale);
            final ModelNode op = new ModelNode();
            op.get(OPERATION_NAME).set("remove");
            op.get(DESCRIPTION).set(resources.getString("infinispan.cache.store.property.remove"));
            op.get(REQUEST_PROPERTIES).setEmptyObject();
            return op;
        }
    };
    private static ResourceBundle getResources(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, (locale == null) ? Locale.getDefault() : locale);
    }

    private static void addCommonCacheChildren(String keyPrefix, ModelNode description, ResourceBundle resources) {
        // information about its child "singleton=*"
        description.get(CHILDREN, ModelKeys.SINGLETON, DESCRIPTION).set(resources.getString(keyPrefix+".singleton"));
        description.get(CHILDREN, ModelKeys.SINGLETON, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.SINGLETON, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.SINGLETON, ALLOWED).setEmptyList();
        description.get(CHILDREN, ModelKeys.SINGLETON, ALLOWED).add("locking").add("transaction").add("eviction").add("expiration");
        description.get(CHILDREN, ModelKeys.SINGLETON, MODEL_DESCRIPTION);
    }

    private static void addStateTransferCacheChildren(String keyPrefix, ModelNode description, ResourceBundle resources) {
        // information about its child "singleton=*"
        description.get(CHILDREN, ModelKeys.SINGLETON, ALLOWED).add("state-transfer");
    }

    private static void addRehashingCacheChildren(String keyPrefix, ModelNode description, ResourceBundle resources) {
        // information about its child "singleton=*"
        description.get(CHILDREN, ModelKeys.SINGLETON, ALLOWED).add("rehashing");
    }

    /**
     * Add the set of request parameters which are common to all cache add operations.
     *
     * @param keyPrefix prefix used to lookup key in resource bundle
     * @param operation the operation ModelNode to add the request properties to
     * @param resources the resource bundle containing keys and their strings
     */
    private static void addCommonCacheAddRequestProperties(String keyPrefix, ModelNode operation, ResourceBundle resources) {

        START.addOperationParameterDescription(resources, keyPrefix, operation);
        BATCHING.addOperationParameterDescription(resources, keyPrefix, operation);
        INDEXING.addOperationParameterDescription(resources, keyPrefix, operation);

        ModelNode requestProperties = operation.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        String lockingPrefix = keyPrefix + "." + "locking" ;
        ModelNode locking = addNode(requestProperties, ModelKeys.LOCKING, resources.getString(lockingPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
            addAttributeDescription(attr, resources, lockingPrefix, locking);
        }

        String transactionPrefix = keyPrefix + "." + "transaction" ;
        ModelNode transaction = addNode(requestProperties, ModelKeys.TRANSACTION, resources.getString(transactionPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
            addAttributeDescription(attr, resources, transactionPrefix, transaction);
        }

        String evictionPrefix = keyPrefix + "." + "eviction" ;
        ModelNode eviction = addNode(requestProperties, ModelKeys.EVICTION, resources.getString(evictionPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
            addAttributeDescription(attr, resources, evictionPrefix, eviction);
        }

        String expirationPrefix = keyPrefix + ".expiration" ;
        ModelNode expiration = addNode(requestProperties, ModelKeys.EXPIRATION, resources.getString(expirationPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
            addAttributeDescription(attr, resources, expirationPrefix, expiration);
         }

        String storePrefix = keyPrefix + "." + "store" ;
        ModelNode store = addNode(requestProperties, ModelKeys.STORE, resources.getString(storePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, store);
        }
        // property needs value type
        addAttributeDescription(PROPERTY, resources, storePrefix, store).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);

        String fileStorePrefix = keyPrefix + "." + "file-store" ;
        ModelNode fileStore = addNode(requestProperties, ModelKeys.FILE_STORE, resources.getString(fileStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, fileStore);
        }
        // property needs value type
        addAttributeDescription(PROPERTY, resources, storePrefix, fileStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        addAttributeDescription(RELATIVE_TO, resources, fileStorePrefix, fileStore);
        addAttributeDescription(PATH, resources, fileStorePrefix, fileStore);

        String jdbcStorePrefix = keyPrefix + ".jdbc-store" ;
        ModelNode jdbcStore = addNode(requestProperties, ModelKeys.JDBC_STORE, resources.getString(jdbcStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, jdbcStore);
        }
        // property needs value type
        addAttributeDescription(PROPERTY, resources, storePrefix, jdbcStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        addAttributeDescription(DATA_SOURCE, resources, jdbcStorePrefix, jdbcStore);

        String remoteStorePrefix = keyPrefix + ".remote-store" ;
        ModelNode remoteStore = addNode(requestProperties, ModelKeys.REMOTE_STORE, resources.getString(remoteStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, remoteStore);
        }
        // property needs value type
        addAttributeDescription(PROPERTY, resources, storePrefix, remoteStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        addAttributeDescription(REMOTE_SERVER, resources, remoteStorePrefix, remoteStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
    }

    /**
     * Add the set of request parameters which are common to all clustered cache add operations.
     *
     * @param keyPrefix prefix used to lookup key in resource bundle
     * @param operation the operation ModelNode to add the request properties to
     * @param resources the resource bundle containing keys and their strings
     */
    private static void addCommonClusteredCacheAddRequestProperties(String keyPrefix, ModelNode operation, ResourceBundle resources) {

        for (AttributeDefinition attr : CommonAttributes.CLUSTERED_CACHE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, keyPrefix, operation);
        }
        // addNode(requestProperties, ModelKeys.MODE, resources.getString(keyPrefix+".mode"), ModelType.STRING, true);
    }

    private static ModelNode addNode(ModelNode parent, String attribute, String description, ModelType type, boolean required) {
       ModelNode node = parent.get(attribute);
       node.get(ModelDescriptionConstants.DESCRIPTION).set(description);
       node.get(ModelDescriptionConstants.TYPE).set(type);
       node.get(ModelDescriptionConstants.REQUIRED).set(required);

       return node;
    }

    /**
     * Add an attribute description to an arbitrary node (and not just REQUEST_PROPERTIES or ATTRIBUTES .
     *
     * @param attribute  the attribute definition defining the attribute
     * @param bundle
     * @param prefix the resource prefix of the attribute
     * @param model the ModelNode to add the description to
     *
     * @return the ModelNode added
     */
    private static ModelNode addAttributeDescription(AttributeDefinition attribute, ResourceBundle bundle, String prefix, ModelNode model) {
       ModelNode node = model.get(attribute.getName());
       node.get(ModelDescriptionConstants.DESCRIPTION).set(bundle.getString(prefix + "." + attribute.getName()));
       node.get(ModelDescriptionConstants.TYPE).set(attribute.getType());
       node.get(ModelDescriptionConstants.REQUIRED).set(attribute.isRequired(model));

       return node;
    }


}
