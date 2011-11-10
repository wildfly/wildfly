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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import java.util.List;

/**
 * User: jpai
 */
class DefaultStatefulBeanAccessTimeoutWriteHandler extends AbstractWriteAttributeHandler<Void> {

    static final DefaultStatefulBeanAccessTimeoutWriteHandler INSTANCE = new DefaultStatefulBeanAccessTimeoutWriteHandler();

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateOrCreateDefaultStatefulBeanAccessTimeoutService(context, model, null);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateOrCreateDefaultStatefulBeanAccessTimeoutService(context, restored, null);
    }

    void updateOrCreateDefaultStatefulBeanAccessTimeoutService(final OperationContext context, final ModelNode model, List<ServiceController<?>> newControllers) throws OperationFailedException {
        final long defaultAccessTimeout = EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_ACCESS_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final ServiceName serviceName = DefaultAccessTimeoutService.STATEFUL_SERVICE_NAME;
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceController sc = registry.getService(serviceName);
        if (sc != null) {
            final DefaultAccessTimeoutService defaultAccessTimeoutService = DefaultAccessTimeoutService.class.cast(sc.getValue());
            defaultAccessTimeoutService.setDefaultAccessTimeout(defaultAccessTimeout);
        } else {
            // create and install the service
            final DefaultAccessTimeoutService defaultAccessTimeoutService = new DefaultAccessTimeoutService(defaultAccessTimeout);
            final ServiceController<?> newService = context.getServiceTarget().addService(serviceName, defaultAccessTimeoutService)
                    .install();
            if (newControllers != null) {
                newControllers.add(newService);
            }
        }
    }
}
