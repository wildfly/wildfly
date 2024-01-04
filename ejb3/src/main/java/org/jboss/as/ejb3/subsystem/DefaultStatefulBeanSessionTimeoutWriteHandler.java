/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.atomic.AtomicLong;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

public class DefaultStatefulBeanSessionTimeoutWriteHandler extends AbstractWriteAttributeHandler<Void> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "defaultStatefulSessionTimeout");
    static final DefaultStatefulBeanSessionTimeoutWriteHandler INSTANCE = new DefaultStatefulBeanSessionTimeoutWriteHandler();
    static final AtomicLong INITIAL_TIMEOUT_VALUE = new AtomicLong(-1);

    private DefaultStatefulBeanSessionTimeoutWriteHandler() {
        super(EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateOrCreateDefaultStatefulBeanSessionTimeoutService(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateOrCreateDefaultStatefulBeanSessionTimeoutService(context, restored);
    }

    void updateOrCreateDefaultStatefulBeanSessionTimeoutService(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final long defaultSessionTimeout = EJB3SubsystemRootResourceDefinition.DEFAULT_STATEFUL_BEAN_SESSION_TIMEOUT.resolveModelAttribute(context, model).asLong();
        final ServiceController<?> sc = context.getServiceRegistry(true).getRequiredService(SERVICE_NAME);
        final AtomicLong existingValue = (AtomicLong) sc.getValue();
        existingValue.set(defaultSessionTimeout);
    }
}
