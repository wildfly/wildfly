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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

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
        ModelNode description = createDescription(resources, "infinispan");
        description.get(HEAD_COMMENT_ALLOWED).set(true);
        description.get(TAIL_COMMENT_ALLOWED).set(true);
        description.get(NAMESPACE).set(Namespace.CURRENT.getUri());

        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, DESCRIPTION).set(resources.getString("infinispan.container"));
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MIN_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MODEL_DESCRIPTION).setEmptyObject();
        return description;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, TYPE).set(ModelType.STRING);
        description.get(REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, DESCRIPTION).set(resources.getString("infinispan.default-container"));
        description.get(REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, REQUIRED).set(false);
        return description;
    }

    static ModelNode getSubsystemDescribeDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.DESCRIBE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        description.get(REPLY_PROPERTIES, TYPE).set(ModelType.LIST);
        description.get(REPLY_PROPERTIES, VALUE_TYPE).set(ModelType.OBJECT);
        return description;
    }

    static ModelNode getSubsystemRemoveDescription(Locale locale) {
        final ResourceBundle bundle = getResources(locale);
        final ModelNode op = new ModelNode();
        op.get(OPERATION_NAME).set(REMOVE);
        op.get(DESCRIPTION).set(bundle.getString("infinispan.remove"));
        op.get(REPLY_PROPERTIES).setEmptyObject();
        op.get(REQUEST_PROPERTIES).setEmptyObject();
        return op;
    }

    // cache containers
    static ModelNode getCacheContainerDescription(Locale locale) {
        String keyPrefix = "infinispan.container" ;
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, keyPrefix);

        // attributes
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // alias is a special case as it has a value type
        addNode(attributes, ModelKeys.ALIAS, resources.getString(keyPrefix+".alias"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);

        // information about its child "singleton=transport"
        description.get(CHILDREN, ModelKeys.SINGLETON, DESCRIPTION).set(resources.getString(keyPrefix+".singleton"));
        description.get(CHILDREN, ModelKeys.SINGLETON, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.SINGLETON, MAX_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.SINGLETON, ALLOWED).setEmptyList().add("transport");
        description.get(CHILDREN, ModelKeys.SINGLETON, MODEL_DESCRIPTION);
        // information about its child "local-cache"
        description.get(CHILDREN, ModelKeys.LOCAL_CACHE, DESCRIPTION).set(resources.getString(keyPrefix+".local-cache"));
        description.get(CHILDREN, ModelKeys.LOCAL_CACHE, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.LOCAL_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.LOCAL_CACHE, MODEL_DESCRIPTION);
        // information about its child "invalidation-cache"
        description.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, DESCRIPTION).set(resources.getString(keyPrefix+".invalidation-cache"));
        description.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.INVALIDATION_CACHE, MODEL_DESCRIPTION);
        // information about its child "local-cache"
        description.get(CHILDREN, ModelKeys.REPLICATED_CACHE, DESCRIPTION).set(resources.getString(keyPrefix+".replicated-cache"));
        description.get(CHILDREN, ModelKeys.REPLICATED_CACHE, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.REPLICATED_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.REPLICATED_CACHE, MODEL_DESCRIPTION);
        // information about its child "local-cache"
        description.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, DESCRIPTION).set(resources.getString(keyPrefix+".distributed-cache"));
        description.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, MIN_OCCURS).set(0);
        description.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.DISTRIBUTED_CACHE, MODEL_DESCRIPTION);

        return description;
    }

    static ModelNode getCacheContainerAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ADD, resources);

        ModelNode requestProperties = description.get(REQUEST_PROPERTIES);
        String keyPrefix = "infinispan.container" ;

        for (AttributeDefinition attr : CommonAttributes.CACHE_CONTAINER_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // alias is a special case as it has a value type
        addNode(requestProperties, ModelKeys.ALIAS, resources.getString("infinispan.container.alias"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);

        return description;
    }

    static ModelNode getAddAliasCommandDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createAddAliasCommandOperationDescription("add-alias", resources);
        ModelNode requestProperties = description.get(REQUEST_PROPERTIES);
        addNode(requestProperties, ModelKeys.NAME, resources.getString("infinispan.container.alias.name"), ModelType.STRING, true);
        return description;
    }

    static ModelNode getRemoveAliasCommandDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createAddAliasCommandOperationDescription("remove-alias", resources);
        ModelNode requestProperties = description.get(REQUEST_PROPERTIES);
        addNode(requestProperties, ModelKeys.NAME, resources.getString("infinispan.container.alias.name"), ModelType.STRING, true);
        return description;
    }

    static ModelNode getCacheContainerRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // local caches
    static ModelNode getLocalCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String keyPrefix = "infinispan.container.local-cache" ;
        ModelNode description = createDescription(resources, keyPrefix);

        // attributes
        keyPrefix = "infinispan.cache" ;
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        // children
        addCommonCacheChildren(keyPrefix, description, resources);

        return description ;
    }

    static ModelNode getLocalCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createLocalCacheOperationDescription(ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        return description;
    }

    // TODO update me
    static ModelNode getCacheRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // invalidation caches
    static ModelNode getInvalidationCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.invalidation-cache");

        // attributes
        String keyPrefix = "infinispan.cache" ;
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        // children
        addCommonCacheChildren(keyPrefix, description, resources);
        addStateTransferCacheChildren(keyPrefix, description, resources);

        return description ;
    }

    static ModelNode getInvalidationCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createInvalidationCacheOperationDescription(ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.clustered-cache";
        addCommonClusteredCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.replicated-cache.state-transfer" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        ModelNode stateTransfer = addNode(requestProperties, ModelKeys.STATE_TRANSFER, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
            addNode(stateTransfer, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    // replicated caches
    static ModelNode getReplicatedCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.replicated-cache");

        // attributes
        String keyPrefix = "infinispan.cache" ;
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        // children
        addCommonCacheChildren(keyPrefix, description, resources);
        addStateTransferCacheChildren(keyPrefix, description, resources);

        return description ;
    }

    static ModelNode getReplicatedCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createReplicatedCacheOperationDescription(ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.clustered-cache";
        addCommonClusteredCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.replicated-cache.state-transfer" ;
        ModelNode requestProperties = description.get(REQUEST_PROPERTIES);
        ModelNode stateTransfer = addNode(requestProperties, ModelKeys.STATE_TRANSFER, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
            addNode(stateTransfer, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    // distributed caches
    static ModelNode getDistributedCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.distributed-cache");

        // attributes
        String keyPrefix = "infinispan.cache" ;
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.CACHE_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        // children
        addCommonCacheChildren(keyPrefix, description, resources);
        addRehashingCacheChildren(keyPrefix, description, resources);

        return description ;
    }

    static ModelNode getDistributedCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDistributedCacheOperationDescription(ModelDescriptionConstants.ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.clustered-cache";
        addCommonClusteredCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.distributed-cache" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.DISTRIBUTED_CACHE_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        keyPrefix = "infinispan.distributed-cache.rehashing" ;
        ModelNode rehashing = addNode(requestProperties, ModelKeys.REHASHING, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.REHASHING_ATTRIBUTES) {
            addNode(rehashing, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    // cache container transport element
    static ModelNode getTransportDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String keyPrefix = "infinispan.container.transport" ;
        ModelNode description = createDescription(resources, keyPrefix);
        // this does have attributes
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getTransportAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createTransportOperationDescription(ADD, resources);

        String keyPrefix = "infinispan.container.transport" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.TRANSPORT_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getTransportRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createTransportOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
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

    private static ModelNode createSubsystemOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan." + operation);
    }

    private static ModelNode createCacheContainerOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container." + operation);
    }

    private static ModelNode createLocalCacheOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container.local-cache." + operation);
    }

    private static ModelNode createInvalidationCacheOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container.invalidation-cache." + operation);
    }

    private static ModelNode createReplicatedCacheOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container.replicated-cache." + operation);
    }

    private static ModelNode createDistributedCacheOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container.distributed-cache." + operation);
    }

    private static ModelNode createTransportOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container.transport." + operation);
    }

    private static ModelNode createLockingOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.cache.locking." + operation);
    }

    private static ModelNode createTransactionOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.cache.transaction." + operation);
    }

    private static ModelNode createEvictionOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.cache.eviction." + operation);
    }

    private static ModelNode createExpirationOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.cache.expiration." + operation);
    }

    private static ModelNode createStateTransferOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.replicated-cache.state-transfer." + operation);
    }

    private static ModelNode createRehashingOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.distributed-cache.rehashing." + operation);
    }

    private static ModelNode createAddAliasCommandOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.container.alias." + operation);
    }

    private static ModelNode createPropertyOperationDescription(String operation, ResourceBundle resources) {
        return createOperationDescription(operation, resources, "infinispan.cache.store.property." + operation);
    }

    private static ModelNode addNode(ModelNode parent, String attribute, String description, ModelType type, boolean required) {
       ModelNode node = parent.get(attribute);
       node.get(ModelDescriptionConstants.DESCRIPTION).set(description);
       node.get(ModelDescriptionConstants.TYPE).set(type);
       node.get(ModelDescriptionConstants.REQUIRED).set(required);

       return node;
    }


    /**
     * Add the set of request parameters which are common to all cache add operations.
     *
     * @param keyPrefix prefix used to lookup key in resource bundle
     * @param operation the operation ModelNode to add the request properties to
     * @param resources the resource bundle containing keys and their strings
     */
    private static void addCommonCacheRequestProperties(String keyPrefix, ModelNode operation, ResourceBundle resources) {

        ModelNode requestProperties = operation.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        // addNode(requestProperties, ModelKeys.NAME, resources.getString(keyPrefix+".name"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.START, resources.getString(keyPrefix+".start"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.BATCHING, resources.getString(keyPrefix+".batching"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.INDEXING, resources.getString(keyPrefix+".indexing"), ModelType.STRING, false);

        String lockingPrefix = keyPrefix + "." + "locking" ;
        ModelNode locking = addNode(requestProperties, ModelKeys.LOCKING, resources.getString(lockingPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
            addNode(locking, attr.getName(), resources.getString(lockingPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        String transactionPrefix = keyPrefix + "." + "transaction" ;
        ModelNode transaction = addNode(requestProperties, ModelKeys.TRANSACTION, resources.getString(transactionPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
            addNode(transaction, attr.getName(), resources.getString(transactionPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        String evictionPrefix = keyPrefix + "." + "eviction" ;
        ModelNode eviction = addNode(requestProperties, ModelKeys.EVICTION, resources.getString(evictionPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
            addNode(eviction, attr.getName(), resources.getString(evictionPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        String expirationPrefix = keyPrefix + ".expiration" ;
        ModelNode expiration = addNode(requestProperties, ModelKeys.EXPIRATION, resources.getString(expirationPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
            addNode(expiration, attr.getName(), resources.getString(expirationPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        String storePrefix = keyPrefix + "." + "store" ;
        ModelNode store = addNode(requestProperties, ModelKeys.STORE, resources.getString(storePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addNode(store, attr.getName(), resources.getString(storePrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // property needs value type
        addNode(store, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);

        String fileStorePrefix = keyPrefix + "." + "file-store" ;
        ModelNode fileStore = addNode(requestProperties, ModelKeys.FILE_STORE, resources.getString(fileStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addNode(fileStore, attr.getName(), resources.getString(storePrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // property needs value type
        addNode(fileStore, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        //
        addNode(fileStore, ModelKeys.RELATIVE_TO, resources.getString(fileStorePrefix+".relative-to"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.PATH, resources.getString(fileStorePrefix+".path"), ModelType.BOOLEAN, false);

        String jdbcStorePrefix = keyPrefix + ".jdbc-store" ;
        ModelNode jdbcStore = addNode(requestProperties, ModelKeys.JDBC_STORE, resources.getString(jdbcStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addNode(jdbcStore, attr.getName(), resources.getString(storePrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // property needs value type
        addNode(jdbcStore, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        //
        addNode(jdbcStore, ModelKeys.DATASOURCE, resources.getString(jdbcStorePrefix+".datasource"), ModelType.STRING, true);

        String remoteStorePrefix = keyPrefix + ".remote-store" ;
        ModelNode remoteStore = addNode(requestProperties, ModelKeys.REMOTE_STORE, resources.getString(remoteStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        for (AttributeDefinition attr : CommonAttributes.STORE_ATTRIBUTES) {
            addNode(remoteStore, attr.getName(), resources.getString(storePrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // property needs value type
        addNode(remoteStore, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        //
        addNode(remoteStore, ModelKeys.REMOTE_SERVER, resources.getString(remoteStorePrefix+".remote-server"), ModelType.LIST, true).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
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
     * Add the set of request parameters which are common to all clustered cache add operations.
     *
     * @param keyPrefix prefix used to lookup key in resource bundle
     * @param operation the operation ModelNode to add the request properties to
     * @param resources the resource bundle containing keys and their strings
     */
    private static void addCommonClusteredCacheRequestProperties(String keyPrefix, ModelNode operation, ResourceBundle resources) {

        ModelNode requestProperties = operation.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.CLUSTERED_CACHE_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(keyPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }
        // addNode(requestProperties, ModelKeys.MODE, resources.getString(keyPrefix+".mode"), ModelType.STRING, true);
    }

    // cache locking element
    static ModelNode getLockingDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String lockingPrefix = "infinispan.cache.locking" ;
        ModelNode description = createDescription(resources, lockingPrefix);
        // this does have attributes
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(lockingPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getLockingAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createLockingOperationDescription(ADD, resources);

        String lockingPrefix = "infinispan.cache.locking" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.LOCKING_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(lockingPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getLockingRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createLockingOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }


    // cache transaction element
    static ModelNode getTransactionDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String transactionPrefix = "infinispan.cache.transaction" ;
        ModelNode description = createDescription(resources, transactionPrefix);
        // this does have attributes
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(transactionPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getTransactionAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createTransactionOperationDescription(ADD, resources);

        String transactionPrefix = "infinispan.cache.transaction" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.TRANSACTION_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(transactionPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getTransactionRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createTransactionOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // cache eviction element
    static ModelNode getEvictionDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String evictionPrefix = "infinispan.cache.eviction" ;
        ModelNode description = createDescription(resources, evictionPrefix);
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(evictionPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getEvictionAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createEvictionOperationDescription(ADD, resources);

        String evictionPrefix = "infinispan.cache.eviction" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.EVICTION_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(evictionPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getEvictionRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createEvictionOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // cache expiration element
    static ModelNode getExpirationDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String expirationPrefix = "infinispan.cache.expiration" ;
        ModelNode description = createDescription(resources, expirationPrefix);
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(expirationPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getExpirationAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createExpirationOperationDescription(ADD, resources);

        String expirationPrefix = "infinispan.cache.expiration" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.EXPIRATION_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(expirationPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getExpirationRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createExpirationOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // cache state transfer element
    static ModelNode getStateTransferDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String stateTransferPrefix = "infinispan.replicated-cache.state-transfer" ;
        ModelNode description = createDescription(resources, stateTransferPrefix);
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(stateTransferPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getStateTransferAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createStateTransferOperationDescription(ADD, resources);

        String expirationPrefix = "infinispan.replicated-cache.state-transfer" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.STATE_TRANSFER_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(expirationPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getStateTransferRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createStateTransferOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // cache rehashing element
    static ModelNode getRehashingDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String rehashingPrefix = "infinispan.distributed-cache.rehashing" ;
        ModelNode description = createDescription(resources, rehashingPrefix);
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        for (AttributeDefinition attr : CommonAttributes.REHASHING_ATTRIBUTES) {
            addNode(attributes, attr.getName(), resources.getString(rehashingPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description ;
    }

    static ModelNode getRehashingAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createRehashingOperationDescription(ADD, resources);

        String rehashingPrefix = "infinispan.distributed-cache.rehashing" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        for (AttributeDefinition attr : CommonAttributes.REHASHING_ATTRIBUTES) {
            addNode(requestProperties, attr.getName(), resources.getString(rehashingPrefix + "." + attr.getName()), attr.getType(), !attr.isAllowNull());
        }

        return description;
    }

    static ModelNode getRehashingRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createRehashingOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    static ModelNode getCacheStorePropertyDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        String propertyPrefix = "infinispan.cache.store.property" ;
        ModelNode description = createDescription(resources, propertyPrefix);
        // this does have attributes
        ModelNode attributes = description.get(ModelDescriptionConstants.ATTRIBUTES);
        addNode(attributes, "value", resources.getString(propertyPrefix+".value"), ModelType.STRING, false);

        return description ;
    }

    static ModelNode getCacheStorePropertyAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createPropertyOperationDescription(ADD, resources);

        String propertyPrefix = "infinispan.cache.store.property" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        addNode(requestProperties, "value", resources.getString(propertyPrefix+".value"), ModelType.STRING, false);

        return description;
    }

    static ModelNode getCacheStorePropertyRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createPropertyOperationDescription(REMOVE, resources);
        description.get(REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

}
