/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;


/**
 * User: jpai
 */
class DefaultSingletonBeanAccessTimeoutWriteHandler extends AbstractWriteAttributeHandler<Void> {

    static final DefaultSingletonBeanAccessTimeoutWriteHandler INSTANCE = new DefaultSingletonBeanAccessTimeoutWriteHandler();

    private DefaultSingletonBeanAccessTimeoutWriteHandler() {
        super(EJB3SubsystemRootResourceDefinition.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateOrCreateDefaultSingletonBeanAccessTimeoutService(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateOrCreateDefaultSingletonBeanAccessTimeoutService(context, restored);
    }

    void updateOrCreateDefaultSingletonBeanAccessTimeoutService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final long defaultAccessTimeout = EJB3SubsystemRootResourceDefinition.DEFAULT_SINGLETON_BEAN_ACCESS_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final ServiceName serviceName = DefaultAccessTimeoutService.SINGLETON_SERVICE_NAME;
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceController<?> sc = registry.getService(serviceName);
        if (sc != null) {
            final DefaultAccessTimeoutService defaultAccessTimeoutService = DefaultAccessTimeoutService.class.cast(sc.getValue());
            defaultAccessTimeoutService.setDefaultAccessTimeout(defaultAccessTimeout);
        } else {
            // create and install the service
            final DefaultAccessTimeoutService defaultAccessTimeoutService = new DefaultAccessTimeoutService(defaultAccessTimeout);
            final ServiceController<?> newService = context.getServiceTarget().addService(serviceName, defaultAccessTimeoutService)
                    .install();
        }
    }
}
