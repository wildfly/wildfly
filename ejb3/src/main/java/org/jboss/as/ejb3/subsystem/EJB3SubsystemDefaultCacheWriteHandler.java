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

package org.jboss.as.ejb3.subsystem;

import java.util.List;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.cache.CacheFactory;
import org.jboss.as.ejb3.cache.CacheFactoryService;
import org.jboss.as.ejb3.cache.impl.backing.clustering.ClusteredBackingCacheEntryStoreSourceService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Paul Ferraro
 */
public class EJB3SubsystemDefaultCacheWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final EJB3SubsystemDefaultCacheWriteHandler SFSB_CACHE =
            new EJB3SubsystemDefaultCacheWriteHandler(CacheFactoryService.DEFAULT_SFSB_CACHE_SERVICE_NAME,
                    null,
                    null,
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SFSB_CACHE);

    public static final EJB3SubsystemDefaultCacheWriteHandler CLUSTERED_SFSB_CACHE =
            new EJB3SubsystemDefaultCacheWriteHandler(CacheFactoryService.DEFAULT_CLUSTERED_SFSB_CACHE_SERVICE_NAME,
                    CacheFactoryService.DEFAULT_SFSB_CACHE_SERVICE_NAME,
                    ClusteredBackingCacheEntryStoreSourceService.getCacheFactoryClusterNameServiceName(null),
                    EJB3SubsystemRootResourceDefinition.DEFAULT_CLUSTERED_SFSB_CACHE);

    private final ServiceName serviceName;
    private final ServiceName defaultServiceName;
    private final ServiceName clusterNameServiceName;
    private final AttributeDefinition attribute;

    public EJB3SubsystemDefaultCacheWriteHandler(ServiceName serviceName, ServiceName defaultServiceName, ServiceName clusterServiceName, AttributeDefinition attribute) {
        super(attribute);
        this.serviceName = serviceName;
        this.defaultServiceName = defaultServiceName;
        this.clusterNameServiceName = clusterServiceName;
        this.attribute = attribute;
    }

    @Override
    protected void validateResolvedValue(String attributeName, ModelNode value) throws OperationFailedException {
        // we're going to validate using the AttributeDefinition in applyModelToRuntime, so don't bother here
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateCacheService(context, model, null);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateCacheService(context, restored, null);
    }

    void updateCacheService(final OperationContext context, final ModelNode model, List<ServiceController<?>> newControllers) throws OperationFailedException {

        ModelNode cacheName = attribute.resolveModelAttribute(context, model);

        ServiceRegistry registry = context.getServiceRegistry(true);
        if (registry.getService(this.serviceName) != null) {
            context.removeService(this.serviceName);
        }
        if ((this.clusterNameServiceName != null) && (registry.getService(this.clusterNameServiceName) != null)) {
            context.removeService(this.clusterNameServiceName);
        }
        ServiceName dependency = cacheName.isDefined() ? CacheFactoryService.getServiceName(cacheName.asString()) : this.defaultServiceName;
        if (dependency != null) {
            ServiceController<?> controller = this.installValueService(context, this.serviceName, CacheFactory.class, dependency);
            if (newControllers != null) {
                newControllers.add(controller);
            }
            if ((this.clusterNameServiceName != null) && cacheName.isDefined()) {
                controller = this.installValueService(context, this.clusterNameServiceName, String.class, ClusteredBackingCacheEntryStoreSourceService.getCacheFactoryClusterNameServiceName(cacheName.asString()));
                if (newControllers != null) {
                    newControllers.add(controller);
                }
            }
        }
    }

    private <T> ServiceController<T> installValueService(final OperationContext context, final ServiceName serviceName, final Class<T> targetClass, final ServiceName dependencyServiceName) {
        final InjectedValue<T> value = new InjectedValue<T>();
        return context.getServiceTarget().addService(serviceName, new ValueService<T>(value))
                .addDependency(dependencyServiceName, targetClass, value)
                .setInitialMode(ServiceController.Mode.ON_DEMAND)
                .install()
        ;
    }
}
