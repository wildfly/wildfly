/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
 * 2110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.interceptors.LoggingInterceptor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Stuart Douglas
 */
class ExceptionLoggingWriteHandler extends AbstractWriteAttributeHandler<Void> {

    static final ExceptionLoggingWriteHandler INSTANCE = new ExceptionLoggingWriteHandler();

    private ExceptionLoggingWriteHandler() {
        super(EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateOrCreateDefaultExceptionLoggingEnabledService(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateOrCreateDefaultExceptionLoggingEnabledService(context, restored);
    }

    void updateOrCreateDefaultExceptionLoggingEnabledService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final boolean enabled = EJB3SubsystemRootResourceDefinition.LOG_EJB_EXCEPTIONS.resolveModelAttribute(context, model).asBoolean();
        final ServiceName serviceName = LoggingInterceptor.LOGGING_ENABLED_SERVICE_NAME;
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceController sc = registry.getService(serviceName);
        if (sc != null) {
            final AtomicBoolean value = (AtomicBoolean) sc.getValue();
            value.set(enabled);
        } else {
            // create and install the service
            final ValueService<AtomicBoolean> service = new ValueService<>(new ImmediateValue<>(new AtomicBoolean(enabled)));
            context.getServiceTarget().addService(serviceName, service)
                    .install();
        }
    }
}
