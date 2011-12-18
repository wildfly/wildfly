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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MODEL_DESCRIPTION;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
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
        description.get(ModelDescriptionConstants.HEAD_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.TAIL_COMMENT_ALLOWED).set(true);
        description.get(ModelDescriptionConstants.NAMESPACE).set(Namespace.CURRENT.getUri());

        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, DESCRIPTION).set(resources.getString("infinispan.container"));
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MIN_OCCURS).set(1);
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(CHILDREN, ModelKeys.CACHE_CONTAINER, MODEL_DESCRIPTION).setEmptyObject();
        return description;
    }

    static ModelNode getSubsystemAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.ADD, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, ModelDescriptionConstants.TYPE).set(ModelType.STRING);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, ModelDescriptionConstants.DESCRIPTION).set(resources.getString("infinispan.default-container"));
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES, ModelKeys.DEFAULT_CACHE_CONTAINER, ModelDescriptionConstants.REQUIRED).set(false);
        return description;
    }

    static ModelNode getSubsystemDescribeDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createSubsystemOperationDescription(ModelDescriptionConstants.DESCRIBE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        description.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.TYPE).set(ModelType.LIST);
        description.get(ModelDescriptionConstants.REPLY_PROPERTIES, ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);
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
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container");

        // information about its child "local-cache"
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.LOCAL_CACHE, ModelDescriptionConstants.DESCRIPTION).set("A local cache resource");
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.LOCAL_CACHE, ModelDescriptionConstants.MIN_OCCURS).set(0);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.LOCAL_CACHE, ModelDescriptionConstants.MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.LOCAL_CACHE, ModelDescriptionConstants.MODEL_DESCRIPTION);
        // information about its child "invalidation-cache"
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.INVALIDATION_CACHE, ModelDescriptionConstants.DESCRIPTION).set("An invalidation cache resource");
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.INVALIDATION_CACHE, ModelDescriptionConstants.MIN_OCCURS).set(0);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.INVALIDATION_CACHE, ModelDescriptionConstants.MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.INVALIDATION_CACHE, ModelDescriptionConstants.MODEL_DESCRIPTION);
        // information about its child "local-cache"
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.REPLICATED_CACHE, ModelDescriptionConstants.DESCRIPTION).set("A replicated cache resource");
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.REPLICATED_CACHE, ModelDescriptionConstants.MIN_OCCURS).set(0);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.REPLICATED_CACHE, ModelDescriptionConstants.MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.REPLICATED_CACHE, ModelDescriptionConstants.MODEL_DESCRIPTION);
        // information about its child "local-cache"
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.DISTRIBUTED_CACHE, ModelDescriptionConstants.DESCRIPTION).set("A distributed cache resource");
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.DISTRIBUTED_CACHE, ModelDescriptionConstants.MIN_OCCURS).set(0);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.DISTRIBUTED_CACHE, ModelDescriptionConstants.MAX_OCCURS).set(Integer.MAX_VALUE);
        description.get(ModelDescriptionConstants.CHILDREN, ModelKeys.DISTRIBUTED_CACHE, ModelDescriptionConstants.MODEL_DESCRIPTION);

        return description;
    }

    static ModelNode getCacheContainerAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ModelDescriptionConstants.ADD, resources);
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        addNode(requestProperties, ModelKeys.DEFAULT_CACHE, resources.getString("infinispan.container.default-cache"), ModelType.STRING, true);
        addNode(requestProperties, ModelKeys.LISTENER_EXECUTOR, resources.getString("infinispan.container.listener-executor"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.EVICTION_EXECUTOR, resources.getString("infinispan.container.eviction-executor"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.REPLICATION_QUEUE_EXECUTOR, resources.getString("infinispan.container.replication-queue-executor"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.JNDI_NAME, resources.getString("infinispan.container.jndi-name"), ModelType.STRING, false);
        addNode(requestProperties, ModelKeys.ALIAS, resources.getString("infinispan.container.alias"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);

        ModelNode transport = addNode(requestProperties, ModelKeys.TRANSPORT, resources.getString("infinispan.container.transport"), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(transport, ModelKeys.SITE, resources.getString("infinispan.container.transport.site"), ModelType.STRING, false);
        addNode(transport, ModelKeys.RACK, resources.getString("infinispan.container.transport.rack"), ModelType.STRING, false);
        addNode(transport, ModelKeys.MACHINE, resources.getString("infinispan.container.transport.machine"), ModelType.STRING, false);
        addNode(transport, ModelKeys.SITE, resources.getString("infinispan.container.transport.site"), ModelType.STRING, false);
        addNode(transport, ModelKeys.EXECUTOR, resources.getString("infinispan.container.transport.executor"), ModelType.STRING, false);
        addNode(transport, ModelKeys.LOCK_TIMEOUT, resources.getString("infinispan.container.transport.lock-timeout"), ModelType.LONG, false);

        // addNode(requestProperties, ModelKeys.CACHE, resources.getString("infinispan.container.cache"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.OBJECT);

        return description;
    }

    static ModelNode getCacheContainerRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ModelDescriptionConstants.REMOVE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // local caches
    static ModelNode getLocalCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.local-cache");
        // need to add in any parameters!

        return description ;
    }

    static ModelNode getLocalCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createLocalCacheOperationDescription(ModelDescriptionConstants.ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        return description;
    }

    // TODO update me
    static ModelNode getCacheRemoveDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createCacheContainerOperationDescription(ModelDescriptionConstants.REMOVE, resources);
        description.get(ModelDescriptionConstants.REQUEST_PROPERTIES).setEmptyObject();
        return description;
    }

    // invalidation caches
    static ModelNode getInvalidationCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.invalidation-cache");
        // need to add in any parameters!

        return description ;
    }

    static ModelNode getInvalidationCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createInvalidationCacheOperationDescription(ModelDescriptionConstants.ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.clustered-cache";
        addCommonClusteredCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.replicated-cache.state-transfer" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        ModelNode stateTransfer = addNode(requestProperties, ModelKeys.STATE_TRANSFER, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(stateTransfer, ModelKeys.ENABLED, resources.getString(keyPrefix+".enabled"), ModelType.BOOLEAN, false);
        addNode(stateTransfer, ModelKeys.TIMEOUT, resources.getString(keyPrefix+".timeout"), ModelType.LONG, false);
        addNode(stateTransfer, ModelKeys.FLUSH_TIMEOUT, resources.getString(keyPrefix+".flush-timeout"), ModelType.LONG, false);

        return description;
    }

    // replicated caches
    static ModelNode getReplicatedCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.replicated-cache");
        // need to add in any parameters!

        return description ;
    }

    static ModelNode getReplicatedCacheAddDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createReplicatedCacheOperationDescription(ModelDescriptionConstants.ADD, resources);

        // need to add in the parameters to the operation!
        String keyPrefix = "infinispan.cache" ;
        addCommonCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.clustered-cache";
        addCommonClusteredCacheRequestProperties(keyPrefix, description, resources);

        keyPrefix = "infinispan.replicated-cache.state-transfer" ;
        ModelNode requestProperties = description.get(ModelDescriptionConstants.REQUEST_PROPERTIES);
        ModelNode stateTransfer = addNode(requestProperties, ModelKeys.STATE_TRANSFER, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(stateTransfer, ModelKeys.ENABLED, resources.getString(keyPrefix+".enabled"), ModelType.BOOLEAN, false);
        addNode(stateTransfer, ModelKeys.TIMEOUT, resources.getString(keyPrefix+".timeout"), ModelType.LONG, false);
        addNode(stateTransfer, ModelKeys.FLUSH_TIMEOUT, resources.getString(keyPrefix+".flush-timeout"), ModelType.LONG, false);

        return description;
    }

    // distributed caches
    static ModelNode getDistributedCacheDescription(Locale locale) {
        ResourceBundle resources = getResources(locale);
        ModelNode description = createDescription(resources, "infinispan.container.distributed-cache");
        // need to add in any parameters!

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

        addNode(requestProperties, ModelKeys.OWNERS, resources.getString(keyPrefix+".owners"), ModelType.INT, false);
        addNode(requestProperties, ModelKeys.VIRTUAL_NODES, resources.getString(keyPrefix+".virtual-nodes"), ModelType.INT, false);
        addNode(requestProperties, ModelKeys.L1_LIFESPAN, resources.getString(keyPrefix+".l1-lifespan"), ModelType.LONG, false);

        ModelNode rehashing = addNode(requestProperties, ModelKeys.REHASHING, resources.getString(keyPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(rehashing, ModelKeys.ENABLED, resources.getString(keyPrefix+".rehashing.enabled"), ModelType.BOOLEAN, false);
        addNode(rehashing, ModelKeys.TIMEOUT, resources.getString(keyPrefix+".rehashing.timeout"), ModelType.LONG, false);

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
            description.get(ModelDescriptionConstants.OPERATION_NAME).set(operation);
        }
        description.get(ModelDescriptionConstants.DESCRIPTION).set(resources.getString(key));
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


    private static ModelNode addNode(ModelNode parent, String attribute, String description,
             ModelType type, boolean required) {
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

        String lockingPrefix = keyPrefix + ".locking" ;
        ModelNode locking = addNode(requestProperties, ModelKeys.LOCKING, resources.getString(lockingPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(locking, ModelKeys.ISOLATION, resources.getString(lockingPrefix+".isolation"), ModelType.STRING, false);
        addNode(locking, ModelKeys.STRIPING, resources.getString(lockingPrefix+".striping"), ModelType.BOOLEAN, false);
        addNode(locking, ModelKeys.ACQUIRE_TIMEOUT, resources.getString(lockingPrefix+".acquire-timeout"), ModelType.LONG, false);
        addNode(locking, ModelKeys.CONCURRENCY_LEVEL, resources.getString(lockingPrefix+".concurrency-level"), ModelType.INT, false);

        String transactionPrefix = keyPrefix + ".transaction" ;
        ModelNode transaction = addNode(requestProperties, ModelKeys.TRANSACTION, resources.getString(transactionPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(transaction, ModelKeys.MODE, resources.getString(transactionPrefix+".mode"), ModelType.STRING, false);
        addNode(transaction, ModelKeys.STOP_TIMEOUT, resources.getString(transactionPrefix+".stop-timeout"), ModelType.INT, false);
        addNode(transaction, ModelKeys.EAGER_LOCKING, resources.getString(transactionPrefix+".eager-locking"), ModelType.STRING, false);

        String evictionPrefix = keyPrefix + ".eviction" ;
        ModelNode eviction = addNode(requestProperties, ModelKeys.EVICTION, resources.getString(evictionPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(eviction, ModelKeys.STRATEGY, resources.getString(evictionPrefix+".strategy"), ModelType.STRING, false);
        addNode(eviction, ModelKeys.MAX_ENTRIES, resources.getString(evictionPrefix+".max-entries"), ModelType.INT, false);

        String expirationPrefix = keyPrefix + ".expiration" ;
        ModelNode expiration = addNode(requestProperties, ModelKeys.EXPIRATION, resources.getString(expirationPrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(expiration, ModelKeys.MAX_IDLE, resources.getString(expirationPrefix+".max-idle"), ModelType.LONG, false);
        addNode(expiration, ModelKeys.LIFESPAN, resources.getString(expirationPrefix+".lifespan"), ModelType.LONG, false);
        addNode(expiration, ModelKeys.INTERVAL, resources.getString(expirationPrefix+".interval"), ModelType.LONG, false);

        String storePrefix = keyPrefix + ".store" ;
        ModelNode store = addNode(requestProperties, ModelKeys.STORE, resources.getString(storePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(store, ModelKeys.SHARED, resources.getString(storePrefix+".shared"), ModelType.BOOLEAN, false);
        addNode(store, ModelKeys.PRELOAD, resources.getString(storePrefix+".preload"), ModelType.BOOLEAN, false);
        addNode(store, ModelKeys.PASSIVATION, resources.getString(storePrefix+".passivation"), ModelType.BOOLEAN, false);
        addNode(store, ModelKeys.FETCH_STATE, resources.getString(storePrefix+".fetch-state"), ModelType.BOOLEAN, false);
        addNode(store, ModelKeys.PURGE, resources.getString(storePrefix+".purge"), ModelType.BOOLEAN, false);
        addNode(store, ModelKeys.SINGLETON, resources.getString(storePrefix+".singleton"), ModelType.BOOLEAN, false);
        addNode(store, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);

        String fileStorePrefix = keyPrefix + ".file-store" ;
        ModelNode fileStore = addNode(requestProperties, ModelKeys.FILE_STORE, resources.getString(fileStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(fileStore, ModelKeys.SHARED, resources.getString(storePrefix+".shared"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.PRELOAD, resources.getString(storePrefix+".preload"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.PASSIVATION, resources.getString(storePrefix+".passivation"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.FETCH_STATE, resources.getString(storePrefix+".fetch-state"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.PURGE, resources.getString(storePrefix+".purge"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.SINGLETON, resources.getString(storePrefix+".singleton"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        //
        addNode(fileStore, ModelKeys.RELATIVE_TO, resources.getString(fileStorePrefix+".relative-to"), ModelType.BOOLEAN, false);
        addNode(fileStore, ModelKeys.PATH, resources.getString(fileStorePrefix+".path"), ModelType.BOOLEAN, false);

        String jdbcStorePrefix = keyPrefix + ".jdbc-store" ;
        ModelNode jdbcStore = addNode(requestProperties, ModelKeys.JDBC_STORE, resources.getString(jdbcStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(jdbcStore, ModelKeys.SHARED, resources.getString(storePrefix+".shared"), ModelType.BOOLEAN, false);
        addNode(jdbcStore, ModelKeys.PRELOAD, resources.getString(storePrefix+".preload"), ModelType.BOOLEAN, false);
        addNode(jdbcStore, ModelKeys.PASSIVATION, resources.getString(storePrefix+".passivation"), ModelType.BOOLEAN, false);
        addNode(jdbcStore, ModelKeys.FETCH_STATE, resources.getString(storePrefix+".fetch-state"), ModelType.BOOLEAN, false);
        addNode(jdbcStore, ModelKeys.PURGE, resources.getString(storePrefix+".purge"), ModelType.BOOLEAN, false);
        addNode(jdbcStore, ModelKeys.SINGLETON, resources.getString(storePrefix+".singleton"), ModelType.BOOLEAN, false);
        addNode(jdbcStore, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        //
        addNode(jdbcStore, ModelKeys.DATASOURCE, resources.getString(jdbcStorePrefix+".datasource"), ModelType.STRING, true);

        String remoteStorePrefix = keyPrefix + ".remote-store" ;
        ModelNode remoteStore = addNode(requestProperties, ModelKeys.REMOTE_STORE, resources.getString(remoteStorePrefix), ModelType.OBJECT, false).get(ModelDescriptionConstants.VALUE_TYPE);
        addNode(remoteStore, ModelKeys.SHARED, resources.getString(storePrefix+".shared"), ModelType.BOOLEAN, false);
        addNode(remoteStore, ModelKeys.PRELOAD, resources.getString(storePrefix+".preload"), ModelType.BOOLEAN, false);
        addNode(remoteStore, ModelKeys.PASSIVATION, resources.getString(storePrefix+".passivation"), ModelType.BOOLEAN, false);
        addNode(remoteStore, ModelKeys.FETCH_STATE, resources.getString(storePrefix+".fetch-state"), ModelType.BOOLEAN, false);
        addNode(remoteStore, ModelKeys.PURGE, resources.getString(storePrefix+".purge"), ModelType.BOOLEAN, false);
        addNode(remoteStore, ModelKeys.SINGLETON, resources.getString(storePrefix+".singleton"), ModelType.BOOLEAN, false);
        addNode(remoteStore, ModelKeys.PROPERTY, resources.getString(storePrefix+".property"), ModelType.LIST, false).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.PROPERTY);
        //
        addNode(remoteStore, ModelKeys.REMOTE_SERVER, resources.getString(remoteStorePrefix+".remote-server"), ModelType.LIST, true).get(ModelDescriptionConstants.VALUE_TYPE).set(ModelType.STRING);
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
        addNode(requestProperties, ModelKeys.MODE, resources.getString(keyPrefix+".mode"), ModelType.STRING, true);
        addNode(requestProperties, ModelKeys.QUEUE_SIZE, resources.getString(keyPrefix+".queue-size"), ModelType.INT, false);
        addNode(requestProperties, ModelKeys.QUEUE_FLUSH_INTERVAL, resources.getString(keyPrefix+".queue-flush-interval"), ModelType.LONG, false);
        addNode(requestProperties, ModelKeys.REMOTE_TIMEOUT, resources.getString(keyPrefix+".remote-timeout"), ModelType.LONG, false);
    }

}
