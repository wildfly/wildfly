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

import java.util.ServiceLoader;

import org.jboss.as.clustering.dmr.ModelNodes;
import org.jboss.as.clustering.infinispan.CacheContainer;
import org.jboss.as.clustering.infinispan.affinity.KeyAffinityServiceFactoryService;
import org.jboss.as.clustering.jgroups.subsystem.ChannelService;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceName;
import org.wildfly.clustering.spi.CacheServiceInstaller;
import org.wildfly.clustering.spi.ClusteredGroupServiceInstaller;
import org.wildfly.clustering.spi.ClusteredCacheServiceInstaller;
import org.wildfly.clustering.spi.GroupServiceInstaller;
import org.wildfly.clustering.spi.LocalCacheServiceInstaller;
import org.wildfly.clustering.spi.LocalGroupServiceInstaller;

/**
 * Remove a cache container, taking care to remove any child cache resources as well.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz (c) 2011 Red Hat, Inc.
 */
public class CacheContainerRemoveHandler extends AbstractRemoveStepHandler {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));

        // remove any existing cache entries
        removeExistingCacheServices(context, address, model);

        final String containerName = address.getLastElement().getValue();

        // need to remove all container-related services started, in reverse order
        context.removeService(KeyAffinityServiceFactoryService.getServiceName(containerName));

        // remove the BinderService entry
        String jndiName = ModelNodes.asString(CacheContainerResourceDefinition.JNDI_NAME.resolveModelAttribute(context, model));
        context.removeService(CacheContainerAddHandler.createCacheContainerBinding(jndiName, containerName).getBinderServiceName());

        // remove the cache container
        context.removeService(EmbeddedCacheManagerService.getServiceName(containerName));
        context.removeService(EmbeddedCacheManagerConfigurationService.getServiceName(containerName));

        if (model.hasDefined(TransportResourceDefinition.PATH.getKey())) {
            removeServices(context, ClusteredGroupServiceInstaller.class, containerName);

            context.removeService(CacheContainerAddHandler.createChannelBinding(containerName).getBinderServiceName());
            context.removeService(ChannelService.getServiceName(containerName));
            context.removeService(ChannelService.getStackServiceName(containerName));
        } else {
            removeServices(context, LocalGroupServiceInstaller.class, containerName);
        }

        String defaultCache = ModelNodes.asString(CacheContainerResourceDefinition.DEFAULT_CACHE.resolveModelAttribute(context, model));

        if ((defaultCache != null) && !defaultCache.equals(CacheContainer.DEFAULT_CACHE_ALIAS)) {
            Class<? extends CacheServiceInstaller> installerClass = model.hasDefined(TransportResourceDefinition.PATH.getKey()) ? ClusteredCacheServiceInstaller.class : LocalCacheServiceInstaller.class;
            for (CacheServiceInstaller installer : ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
                for (ServiceName serviceName : installer.getServiceNames(containerName, CacheContainer.DEFAULT_CACHE_ALIAS)) {
                    context.removeService(serviceName);
                }
            }
        }
    }

    private static <I extends GroupServiceInstaller> void removeServices(OperationContext context, Class<I> installerClass, String group) {
        for (I installer: ServiceLoader.load(installerClass, installerClass.getClassLoader())) {
            for (ServiceName name: installer.getServiceNames(group)) {
                context.removeService(name);
            }
        }
    }

    /**
     * Method to re-install any services associated with existing local caches.
     *
     * @param context
     * @param operation
     * @param model
     * @throws OperationFailedException
     */
    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();

        // re-install the cache container services
        CacheContainerAddHandler.installRuntimeServices(context, operation, model, verificationHandler);

        // re-install any existing cache services
        reinstallExistingCacheServices(context, address, model, verificationHandler);
    }


    /**
     * Method to reinstall any services associated with existing caches.
     *
     * @param context
     * @param containerModel
     * @param containerName
     * @throws OperationFailedException
     */
    private static void reinstallExistingCacheServices(OperationContext context, PathAddress containerAddress, ModelNode containerModel, ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        for (CacheType type: CacheType.values()) {
            CacheAddHandler addHandler = type.getAddHandler();
            if (containerModel.hasDefined(type.pathElement().getKey())) {
                for (Property property: containerModel.get(type.pathElement().getKey()).asPropertyList()) {
                    ModelNode addOperation = Util.createAddOperation(containerAddress.append(type.pathElement(property.getName())));
                    addHandler.installRuntimeServices(context, addOperation, containerModel, property.getValue(), verificationHandler);
                }
            }
        }
    }

    /**
     * Method to remove any services associated with existing caches.
     */
    private static void removeExistingCacheServices(OperationContext context, PathAddress containerAddress, ModelNode containerModel) throws OperationFailedException {

        for (CacheType type: CacheType.values()) {
            CacheAddHandler addHandler = type.getAddHandler();
            if (containerModel.hasDefined(type.pathElement().getKey())) {
                for (Property property: containerModel.get(type.pathElement().getKey()).asPropertyList()) {
                    ModelNode removeOperation = Util.createRemoveOperation(containerAddress.append(type.pathElement(property.getName())));
                    addHandler.removeRuntimeServices(context, removeOperation, containerModel, property.getValue());
                }
            }
        }
    }
}
