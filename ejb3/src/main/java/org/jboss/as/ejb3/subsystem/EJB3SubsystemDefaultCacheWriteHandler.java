/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.ejb3.component.stateful.cache.StatefulSessionBeanCacheProvider;
import org.jboss.dmr.ModelNode;
import org.wildfly.subsystem.service.ServiceDependency;
import org.wildfly.subsystem.service.capability.CapabilityServiceInstaller;

/**
 * @author Paul Ferraro
 */
public class EJB3SubsystemDefaultCacheWriteHandler extends AbstractWriteAttributeHandler<Void> {

    static final EJB3SubsystemDefaultCacheWriteHandler SFSB_CACHE = new EJB3SubsystemDefaultCacheWriteHandler(EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_CACHE);

    static final EJB3SubsystemDefaultCacheWriteHandler SFSB_PASSIVATION_DISABLED_CACHE = new EJB3SubsystemDefaultCacheWriteHandler(EJB3SubsystemRootResourceDefinition.PASSIVATION_DISABLED_STATEFUL_BEAN_CACHE);

    private final RuntimeCapability<Void> capability;
    private final AtomicReference<Consumer<OperationContext>> remover = new AtomicReference<>();

    public EJB3SubsystemDefaultCacheWriteHandler(RuntimeCapability<Void> capability) {
        this.capability = capability;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        this.updateCacheService(context, resolvedValue.asStringOrNull());
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        this.updateCacheService(context, valueToRestore.asStringOrNull());
    }

    void updateCacheService(final OperationContext context, String cacheName) {

        Consumer<OperationContext> remover = this.remover.getAndSet(null);
        if (remover != null) {
            remover.accept(context);
        }
        if (cacheName != null) {
            this.remover.set(CapabilityServiceInstaller.builder(this.capability, ServiceDependency.on(StatefulSessionBeanCacheProvider.SERVICE_DESCRIPTOR, cacheName)).build().install(context));
        }
    }
}
