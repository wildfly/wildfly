/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.DefaultAccessTimeoutService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Stuart Douglas
 */
public class DefaultSessionBeanAccessTimeoutWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition attribute;
    private final ServiceName serviceName;

    public DefaultSessionBeanAccessTimeoutWriteHandler(final AttributeDefinition attribute, final ServiceName serviceName) {
        super(attribute);
        this.attribute = attribute;
        this.serviceName = serviceName;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        applyModelToRuntime(context, model);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        applyModelToRuntime(context, restored);
    }


    private void applyModelToRuntime(OperationContext context, final ModelNode model) throws OperationFailedException {
        long timeout = attribute.resolveModelAttribute(context, model).asLong();
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        ServiceController<DefaultAccessTimeoutService> controller = (ServiceController<DefaultAccessTimeoutService>) serviceRegistry.getService(serviceName);
        if (controller != null) {
            DefaultAccessTimeoutService service = controller.getValue();
            if (service != null) {
                service.setDefaultAccessTimeout(timeout);
            }
        }
    }

}
