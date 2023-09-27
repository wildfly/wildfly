/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.interceptors.LoggingInterceptor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

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
            final ServiceBuilder<?> sb = context.getServiceTarget().addService(serviceName);
            sb.setInstance(new ValueService(new AtomicBoolean(enabled))).install();
        }
    }

    private static final class ValueService implements Service<AtomicBoolean> {
        private final AtomicBoolean value;

        public ValueService(final AtomicBoolean value) {
            this.value = value;
        }

        public void start(final StartContext context) {
            // noop
        }

        public void stop(final StopContext context) {
            // noop
        }

        public AtomicBoolean getValue() throws IllegalStateException {
            return value;
        }
    }
}
