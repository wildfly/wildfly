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

import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.*;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.NAME;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.PATH;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.RELATIVE_TO;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.START;
import static org.jboss.as.clustering.infinispan.subsystem.CommonAttributes.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
* @author Paul Ferraro
* @author Richard Achmatowicz (c) Red Hat Inc. 2011
*/
public class InfinispanDescriptions {

    public static final String RESOURCE_NAME = InfinispanDescriptions.class.getPackage().getName() + ".LocalDescriptions";

    private InfinispanDescriptions() {
        // Hide
    }

    // subsystems
    static ModelNode getSubsystemDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode subsystem = createDescription(resources, "infinispan");
        subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
        subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
        subsystem.get(NAMESPACE).set(Namespace.CURRENT.getUri());
        subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, DESCRIPTION).set(resources.getString("infinispan.container"));
        subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MIN_OCCURS).set(1);
        subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MAX_OCCURS).set(Integer.MAX_VALUE);
        subsystem.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MODEL_DESCRIPTION).setEmptyObject();
        return subsystem;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.add");
        DEFAULT_CACHE_CONTAINER.addOperationParameterDescription(resources, "infinispan", op);
        return op;
    }

    static ModelNode getSubsystemDescribeDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(DESCRIBE, resources, "infinispan.describe");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        op.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        op.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
        return op;
    }

    static ModelNode getSubsystemRemoveDescription(Locale locale) {
        final ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.remove");
        op.get(REPLY_PROPERTIES).setEmptyObject();
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache containers
    static ModelNode getCacheContainerDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String keyPrefix = "infinispan.container" ;
        final ModelNode container = createDescription(resources, keyPrefix);
        // attributes
        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, keyPrefix, container);
        }
        // information about its child "transport=TRANSPORT"
        container.get(CHILDREN, ModelKeys.TRANSPORT, DESCRIPTION).set(resources.getString(keyPrefix + ".transport"));
        container.get(CHILDREN, ModelKeys.TRANSPORT, MIN_OCCURS).set(0);
        container.get(CHILDREN, ModelKeys.TRANSPORT, MAX_OCCURS).set(1);
        container.get(CHILDREN, ModelKeys.TRANSPORT, ALLOWED).setEmptyList().add(ModelKeys.TRANSPORT_NAME);
        container.get(CHILDREN, ModelKeys.TRANSPORT, MODEL_DESCRIPTION);
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

    static ModelNode getCacheContainerAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.container.add");
        // request parameters
        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.container", op);
        }
        return op;
    }

    static ModelNode getCacheContainerRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.container.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getAddAliasCommandDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription("add-alias", resources, "infinispan.container.alias.add-alias");
        NAME.addOperationParameterDescription(resources, "infinispan.container.alias", op);
        return op;
    }

    static ModelNode getRemoveAliasCommandDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription("remove-alias", resources, "infinispan.container.alias.remove-alias");
        NAME.addOperationParameterDescription(resources, "infinispan.container.alias", op);
        return op;
    }


    // local caches
    static ModelNode getLocalCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode cache = createDescription(resources, "infinispan.container.local-cache");
        // attributes
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
        }
        // children
        addCommonCacheChildren("infinispan.cache", cache, resources);
        return cache ;
    }

    static ModelNode getLocalCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.container.local-cache.add");
        addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
        return op;
    }

    // invalidation caches
    static ModelNode getInvalidationCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode cache = createDescription(resources, "infinispan.container.invalidation-cache");
        // attributes
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
        }
        // children
        addCommonCacheChildren("infinispan.cache", cache, resources);
        return cache ;
     }

    static ModelNode getInvalidationCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.container.invalidation-cache.add");
        // parameters
        addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
        addCommonClusteredCacheAddRequestProperties("infinispan.clustered-cache", op, resources);
        return op;
    }

    // replicated caches
    static ModelNode getReplicatedCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode cache = createDescription(resources, "infinispan.container.replicated-cache");
        // attributes
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
        }
        // children
        addCommonCacheChildren("infinispan.cache", cache, resources);
        addStateTransferCacheChildren("infinispan.replicated-cache", cache, resources);
        return cache ;
    }

    static ModelNode getReplicatedCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.container.replicated-cache.add");
        // parameters
        addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
        addCommonClusteredCacheAddRequestProperties("infinispan.clustered-cache", op, resources);
        // nested resource initialization
        STATE_TRANSFER_OBJECT.addOperationParameterDescription(resources, "infinispan.replicated-cache", op);
        return op;
    }

    // distributed caches
    static ModelNode getDistributedCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode cache = createDescription(resources, "infinispan.container.distributed-cache");
        // attributes
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache", cache);
        }
        // children
        addCommonCacheChildren("infinispan.cache", cache, resources);
        addStateTransferCacheChildren("infinispan.replicated-cache", cache, resources);
        return cache ;
    }

    static ModelNode getDistributedCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.container.distributed-cache.add");
        // parameters
        addCommonCacheAddRequestProperties("infinispan.cache", op, resources);
        addCommonClusteredCacheAddRequestProperties("infinispan.clustered-cache", op, resources);
        for (AttributeDefinition attr : CommonAttributes.DISTRIBUTED_CACHE_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.distributed-cache", op);
        }
        // nested resource initialization
        STATE_TRANSFER_OBJECT.addOperationParameterDescription(resources, "infinispan.replicated-cache", op);
        return op;
    }

    // TODO update me
    static ModelNode getCacheRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.cache.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache container transport element
    static ModelNode getTransportDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode transport = createDescription(resources, "infinispan.container.transport");
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.container.transport", transport);
        }
        return transport ;
    }

    static ModelNode getTransportAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.container.transport.add");
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.container.transport", op);
        }
        return op;
    }

    static ModelNode getTransportRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.container.transport.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache locking element
    static ModelNode getLockingDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode locking = createDescription(resources, "infinispan.cache.locking");
        for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache.locking", locking);
        }
        return locking ;
    }

    static ModelNode getLockingAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.cache.locking.add");
        for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.cache.locking", op);
        }
        return op;
    }

    static ModelNode getLockingRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.cache.locking.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache transaction element
    static ModelNode getTransactionDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode transaction = createDescription(resources, "infinispan.cache.transaction");
        for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache.transaction", transaction);
        }
        return transaction ;
    }

    static ModelNode getTransactionAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.cache.transaction.add");
        for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.cache.transaction", op);
        }
        return op;
    }

    static ModelNode getTransactionRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.cache.transaction.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache eviction element
    static ModelNode getEvictionDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode eviction = createDescription(resources, "infinispan.cache.eviction");
        for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache.eviction", eviction);
        }
        return eviction ;
    }

    static ModelNode getEvictionAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.cache.eviction.add");
        for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.cache.eviction", op);
        }
        return op;
    }

    static ModelNode getEvictionRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.cache.eviction.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache expiration elemet
    static ModelNode getExpirationDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode expiration = createDescription(resources, "infinispan.cache.expiration");
        for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.cache.expiration", expiration);
        }
        return expiration ;
    }

    static ModelNode getExpirationAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.cache.expiration.add");
        for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.cache.expiration", op);
        }
        return op;
    }

    static ModelNode getExpirationRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.cache.expiration.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache state transfer element
    static ModelNode getStateTransferDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode stateTransfer = createDescription(resources, "infinispan.replicated-cache.state-transfer");
        for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
            attr.addResourceAttributeDescription(resources, "infinispan.replicated-cache.state-transfer", stateTransfer);
        }
        return stateTransfer ;
    }

    static ModelNode getStateTransferAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.replicated-cache.state-transfer.add");
        for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
            attr.addOperationParameterDescription(resources, "infinispan.replicated-cache.state-transfer", op);
        }
        return op;
    }

    static ModelNode getStateTransferRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.replicated-cache.state-transfer.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    static ModelNode getCacheStorePropertyDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode storeProperty = createDescription(resources, "infinispan.cache.store.property");
        VALUE.addResourceAttributeDescription(resources, "infinispan.cache.store.property", storeProperty);
        return storeProperty ;
    }

    static ModelNode getCacheStorePropertyAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(ADD, resources, "infinispan.cache.store.property.add");
        VALUE.addOperationParameterDescription(resources, "infinispan.cache.store.property", op);
        return op;
    }

    static ModelNode getCacheStorePropertyRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        final ModelNode op = createOperationDescription(REMOVE, resources, "infinispan.cache.store.property.remove");
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    private static ResourceBundle getResources(Locale locale) {
        return ResourceBundle.getBundle(RESOURCE_NAME, (locale == null) ? Locale.getDefault() : locale);
    }

    private static ModelNode createDescription(ResourceBundle resources, String key) {
        return createOperationDescription(null, resources, key);
    }

    private static ModelNode createOperationDescription(String operation, ResourceBundle resources, String key) {
        ModelNode description = new ModelNode();
        if (operation != null) {
            description.get(OPERATION_NAME).set(operation);
        }
        description.get(DESCRIPTION).set(resources.getString(key));
        return description;
    }

    private static void addCommonCacheChildren(String keyPrefix, ModelNode description, ResourceBundle resources) {
        // information about its child "locking=LOCKING"
        description.get(CHILDREN, ModelKeys.LOCKING, DESCRIPTION).set(resources.getString(keyPrefix+".locking"));
        description.get(CHILDREN, ModelKeys.LOCKING, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.LOCKING, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.LOCKING, ALLOWED).setEmptyList();
        description.get(CHILDREN, ModelKeys.LOCKING, ALLOWED).add(ModelKeys.LOCKING_NAME);
        description.get(CHILDREN, ModelKeys.LOCKING, MODEL_DESCRIPTION);
        // information about its child "transaction=TRANSACTION"
        description.get(CHILDREN, ModelKeys.TRANSACTION, DESCRIPTION).set(resources.getString(keyPrefix+".transaction"));
        description.get(CHILDREN, ModelKeys.TRANSACTION, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.TRANSACTION, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.TRANSACTION, ALLOWED).setEmptyList();
        description.get(CHILDREN, ModelKeys.TRANSACTION, ALLOWED).add(ModelKeys.TRANSACTION_NAME);
        description.get(CHILDREN, ModelKeys.TRANSACTION, MODEL_DESCRIPTION);
        // information about its child "eviction=EVICTION"
        description.get(CHILDREN, ModelKeys.EVICTION, DESCRIPTION).set(resources.getString(keyPrefix+".eviction"));
        description.get(CHILDREN, ModelKeys.EVICTION, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.EVICTION, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.EVICTION, ALLOWED).setEmptyList();
        description.get(CHILDREN, ModelKeys.EVICTION, ALLOWED).add(ModelKeys.EVICTION_NAME);
        description.get(CHILDREN, ModelKeys.EVICTION, MODEL_DESCRIPTION);
        // information about its child "expiration=EXPIRATION"
        description.get(CHILDREN, ModelKeys.EXPIRATION, DESCRIPTION).set(resources.getString(keyPrefix+".expiration"));
        description.get(CHILDREN, ModelKeys.EXPIRATION, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.EXPIRATION, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.EXPIRATION, ALLOWED).setEmptyList();
        description.get(CHILDREN, ModelKeys.EXPIRATION, ALLOWED).add(ModelKeys.EXPIRATION_NAME);
        description.get(CHILDREN, ModelKeys.EXPIRATION, MODEL_DESCRIPTION);
     }

    private static void addStateTransferCacheChildren(String keyPrefix, ModelNode description, ResourceBundle resources) {
        // information about its child "singleton=*"
        description.get(CHILDREN, ModelKeys.STATE_TRANSFER, DESCRIPTION).set(resources.getString(keyPrefix+".state-transfer"));
        description.get(CHILDREN, ModelKeys.STATE_TRANSFER, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.STATE_TRANSFER, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.STATE_TRANSFER, ALLOWED).setEmptyList();
        description.get(CHILDREN, ModelKeys.STATE_TRANSFER, ALLOWED).add(ModelKeys.STATE_TRANSFER_NAME);
        description.get(CHILDREN, ModelKeys.STATE_TRANSFER, MODEL_DESCRIPTION);
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

        // these really need to be removed - we never pass such OBJECTs as parameters to add
        // as they are now defined as child resources
        LOCKING_OBJECT.addOperationParameterDescription(resources, keyPrefix , operation) ;
        TRANSACTION_OBJECT.addOperationParameterDescription(resources, keyPrefix , operation) ;
        EVICTION_OBJECT.addOperationParameterDescription(resources, keyPrefix , operation) ;
        EXPIRATION_OBJECT.addOperationParameterDescription(resources, keyPrefix , operation) ;

        ModelNode requestProperties = operation.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        String storePrefix = keyPrefix + "." + "store" ;
        ModelNode store = addNode(requestProperties, ModelKeys.STORE, resources.getString(storePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, store);
        }
        // property needs value type
        addAttributeDescription(CommonAttributes.PROPERTIES, resources, storePrefix, store).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);

        String fileStorePrefix = keyPrefix + "." + "file-store" ;
        ModelNode fileStore = addNode(requestProperties, ModelKeys.FILE_STORE, resources.getString(fileStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, fileStore);
        }
        // property needs value type
        addAttributeDescription(CommonAttributes.PROPERTIES, resources, storePrefix, fileStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        addAttributeDescription(RELATIVE_TO, resources, fileStorePrefix, fileStore);
        addAttributeDescription(PATH, resources, fileStorePrefix, fileStore);

        String jdbcStorePrefix = keyPrefix + ".jdbc-store" ;
        ModelNode jdbcStore = addNode(requestProperties, ModelKeys.JDBC_STORE, resources.getString(jdbcStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, jdbcStore);
        }
        // property needs value type
        addAttributeDescription(CommonAttributes.PROPERTIES, resources, storePrefix, jdbcStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        addAttributeDescription(DATA_SOURCE, resources, jdbcStorePrefix, jdbcStore);

        String remoteStorePrefix = keyPrefix + ".remote-store" ;
        ModelNode remoteStore = addNode(requestProperties, ModelKeys.REMOTE_STORE, resources.getString(remoteStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addAttributeDescription(attr, resources, storePrefix, remoteStore);
        }
        // property needs value type
        addAttributeDescription(CommonAttributes.PROPERTIES, resources, storePrefix, remoteStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        addAttributeDescription(REMOTE_SERVERS, resources, remoteStorePrefix, remoteStore).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
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
