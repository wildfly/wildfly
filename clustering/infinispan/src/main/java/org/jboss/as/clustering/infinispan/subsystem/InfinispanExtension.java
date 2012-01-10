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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry.EntryType;

/**
 * Defines the Infinispan subsystem and its addressable resources.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanExtension implements Extension {

    static final String SUBSYSTEM_NAME = "infinispan";

    private static final PathElement containerPath = PathElement.pathElement(ModelKeys.CACHE_CONTAINER);
    private static final PathElement localCachePath = PathElement.pathElement(ModelKeys.LOCAL_CACHE);
    private static final PathElement invalidationCachePath = PathElement.pathElement(ModelKeys.INVALIDATION_CACHE);
    private static final PathElement replicatedCachePath = PathElement.pathElement(ModelKeys.REPLICATED_CACHE);
    private static final PathElement distributedCachePath = PathElement.pathElement(ModelKeys.DISTRIBUTED_CACHE);
    private static final PathElement transportPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.TRANSPORT);

    private static final PathElement lockingPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.LOCKING);
    private static final PathElement transactionPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.TRANSACTION);
    private static final PathElement evictionPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.EVICTION);
    private static final PathElement expirationPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.EXPIRATION);
    private static final PathElement stateTransferPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.STATE_TRANSFER);
    private static final PathElement rehashingPath = PathElement.pathElement(ModelKeys.SINGLETON, ModelKeys.REHASHING);
    private static final PathElement storePropertyPath = PathElement.pathElement(ModelKeys.PROPERTY);


    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, Namespace.CURRENT.getMajorVersion(), Namespace.CURRENT.getMinorVersion());
        subsystem.registerXMLElementWriter(Namespace.CURRENT.getWriter());

        ManagementResourceRegistration registration = subsystem.registerSubsystemModel(InfinispanSubsystemProviders.SUBSYSTEM);
        registration.registerOperationHandler(ADD, InfinispanSubsystemAdd.INSTANCE, InfinispanSubsystemProviders.SUBSYSTEM_ADD, false);
        registration.registerOperationHandler(DESCRIBE, InfinispanSubsystemDescribe.INSTANCE, InfinispanSubsystemProviders.SUBSYSTEM_DESCRIBE, false, EntryType.PRIVATE);
        registration.registerOperationHandler(REMOVE, ReloadRequiredRemoveStepHandler.INSTANCE, InfinispanSubsystemProviders.SUBSYSTEM_REMOVE, false);

        ManagementResourceRegistration container = registration.registerSubModel(containerPath, InfinispanSubsystemProviders.CACHE_CONTAINER);
        container.registerOperationHandler(ADD, CacheContainerAdd.INSTANCE, InfinispanSubsystemProviders.CACHE_CONTAINER_ADD, false);
        container.registerOperationHandler(REMOVE, CacheContainerRemove.INSTANCE, InfinispanSubsystemProviders.CACHE_CONTAINER_REMOVE, false);
        container.registerOperationHandler("add-alias", AddAliasCommand.INSTANCE, InfinispanSubsystemProviders.ADD_ALIAS, false);
        container.registerOperationHandler("remove-alias", RemoveAliasCommand.INSTANCE, InfinispanSubsystemProviders.REMOVE_ALIAS, false);
        CacheContainerWriteAttributeHandler.INSTANCE.registerAttributes(container);

        // add /subsystem=infinispan/cache-container=*/singleton=transport:write-attribute
        final ManagementResourceRegistration transport = container.registerSubModel(transportPath,InfinispanSubsystemProviders.TRANSPORT);
        transport.registerOperationHandler(ADD, TransportAdd.INSTANCE, InfinispanSubsystemProviders.TRANSPORT_ADD, false);
        transport.registerOperationHandler(REMOVE, TransportRemove.INSTANCE, InfinispanSubsystemProviders.TRANSPORT_REMOVE, false);
        TransportWriteAttributeHandler.INSTANCE.registerAttributes(transport);

        // add /subsystem=infinispan/cache-container=*/local-cache=*
        ManagementResourceRegistration local = container.registerSubModel(localCachePath, InfinispanSubsystemProviders.LOCAL_CACHE);
        local.registerOperationHandler(ADD, LocalCacheAdd.INSTANCE, InfinispanSubsystemProviders.LOCAL_CACHE_ADD, false);
        local.registerOperationHandler(REMOVE, CacheRemove.INSTANCE, InfinispanSubsystemProviders.CACHE_REMOVE, false);
        registerCommonCacheAttributeHandlers(local);

        // add /subsystem=infinispan/cache-container=*/invalidation-cache=*
        ManagementResourceRegistration invalidation = container.registerSubModel(invalidationCachePath, InfinispanSubsystemProviders.INVALIDATION_CACHE);
        invalidation.registerOperationHandler(ADD, InvalidationCacheAdd.INSTANCE, InfinispanSubsystemProviders.INVALIDATION_CACHE_ADD, false);
        invalidation.registerOperationHandler(REMOVE, CacheRemove.INSTANCE, InfinispanSubsystemProviders.CACHE_REMOVE, false);
        registerCommonCacheAttributeHandlers(invalidation);

        // add /subsystem=infinispan/cache-container=*/replicated-cache=*
        ManagementResourceRegistration replicated = container.registerSubModel(replicatedCachePath, InfinispanSubsystemProviders.REPLICATED_CACHE);
        replicated.registerOperationHandler(ADD, ReplicatedCacheAdd.INSTANCE, InfinispanSubsystemProviders.REPLICATED_CACHE_ADD, false);
        replicated.registerOperationHandler(REMOVE, CacheRemove.INSTANCE, InfinispanSubsystemProviders.CACHE_REMOVE, false);
        registerCommonCacheAttributeHandlers(replicated);

        // add /subsystem=infinispan/cache-container=*/distributed-cache=*
        ManagementResourceRegistration distributed = container.registerSubModel(distributedCachePath, InfinispanSubsystemProviders.DISTRIBUTED_CACHE);
        distributed.registerOperationHandler(ADD, DistributedCacheAdd.INSTANCE, InfinispanSubsystemProviders.DISTRIBUTED_CACHE_ADD, false);
        distributed.registerOperationHandler(REMOVE, CacheRemove.INSTANCE, InfinispanSubsystemProviders.CACHE_REMOVE, false);
        registerCommonCacheAttributeHandlers(distributed);
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            if (namespace != Namespace.UNKNOWN) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), namespace.getReader());
            }
        }
    }

    private void registerCommonCacheAttributeHandlers(ManagementResourceRegistration resource) {
        // register the singleton=locking handlers
        final ManagementResourceRegistration locking = resource.registerSubModel(lockingPath, InfinispanSubsystemProviders.LOCKING);
        locking.registerOperationHandler(ADD, CacheConfigOperationHandlers.LOCKING_ADD, InfinispanSubsystemProviders.LOCKING_ADD);
        locking.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.LOCKING_REMOVE);
        CacheConfigOperationHandlers.LOCKING_ATTR.registerAttributes(locking);

        // register the singleton=transaction handlers
        final ManagementResourceRegistration transaction = resource.registerSubModel(transactionPath, InfinispanSubsystemProviders.TRANSACTION);
        transaction.registerOperationHandler(ADD, CacheConfigOperationHandlers.TRANSACTION_ADD, InfinispanSubsystemProviders.TRANSACTION_ADD);
        transaction.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.TRANSACTION_REMOVE);
        CacheConfigOperationHandlers.TRANSACTION_ATTR.registerAttributes(transaction);

        // register the singleton=eviction handlers
        final ManagementResourceRegistration eviction = resource.registerSubModel(evictionPath, InfinispanSubsystemProviders.EVICTION);
        eviction.registerOperationHandler(ADD, CacheConfigOperationHandlers.EVICTION_ADD, InfinispanSubsystemProviders.EVICTION_ADD);
        eviction.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.EVICTION_REMOVE);
        CacheConfigOperationHandlers.EVICTION_ATTR.registerAttributes(eviction);

        // register the singleton=expiration handlers
        final ManagementResourceRegistration expiration = resource.registerSubModel(expirationPath, InfinispanSubsystemProviders.EXPIRATION);
        expiration.registerOperationHandler(ADD, CacheConfigOperationHandlers.EXPIRATION_ADD, InfinispanSubsystemProviders.EXPIRATION_ADD);
        expiration.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.EXPIRATION_REMOVE);
        CacheConfigOperationHandlers.LOCKING_ATTR.registerAttributes(expiration);

        // register the singleton=state-transfer handlers
        final ManagementResourceRegistration stateTransfer = resource.registerSubModel(stateTransferPath, InfinispanSubsystemProviders.STATE_TRANSFER);
        stateTransfer.registerOperationHandler(ADD, CacheConfigOperationHandlers.STATE_TRANSFER_ADD, InfinispanSubsystemProviders.STATE_TRANSFER_ADD);
        stateTransfer.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.STATE_TRANSFER_REMOVE);
        CacheConfigOperationHandlers.STATE_TRANSFER_ATTR.registerAttributes(stateTransfer);

        // register the singleton=rehashing handlers
        final ManagementResourceRegistration rehashing = resource.registerSubModel(rehashingPath, InfinispanSubsystemProviders.REHASHING);
        rehashing.registerOperationHandler(ADD, CacheConfigOperationHandlers.REHASHING_ADD, InfinispanSubsystemProviders.REHASHING_ADD);
        rehashing.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.REHASHING_REMOVE);
        CacheConfigOperationHandlers.STATE_TRANSFER_ATTR.registerAttributes(rehashing);

    }

    static void createPropertyRegistration(final ManagementResourceRegistration parent) {
        final ManagementResourceRegistration registration = parent.registerSubModel(storePropertyPath, InfinispanSubsystemProviders.STORE_PROPERTY);
        registration.registerOperationHandler(ADD, CacheConfigOperationHandlers.STORE_PROPERTY_ADD, InfinispanSubsystemProviders.STORE_PROPERTY_ADD);
        registration.registerOperationHandler(REMOVE, CacheConfigOperationHandlers.REMOVE, InfinispanSubsystemProviders.STORE_PROPERTY_REMOVE);
        registration.registerReadWriteAttribute("value", null, CacheConfigOperationHandlers.STORE_PROPERTY_ATTR, EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES));
    }

}
