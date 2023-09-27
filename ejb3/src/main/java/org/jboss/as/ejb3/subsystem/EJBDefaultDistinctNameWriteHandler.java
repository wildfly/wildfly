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
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author Stuart Douglas
 */
class EJBDefaultDistinctNameWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final EJBDefaultDistinctNameWriteHandler INSTANCE = new EJBDefaultDistinctNameWriteHandler(EJB3SubsystemRootResourceDefinition.DEFAULT_DISTINCT_NAME);

    private final AttributeDefinition attributeDefinition;

    private EJBDefaultDistinctNameWriteHandler(final AttributeDefinition attributeDefinition) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDefaultDistinctName(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDefaultDistinctName(context, restored);
    }

    void updateDefaultDistinctName(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode defaultDistinctName = this.attributeDefinition.resolveModelAttribute(context, model);
        final ServiceRegistry registry = context.getServiceRegistry(true);

        final ServiceController<?> existingDefaultLocalEJBReceiverServiceController = registry.getService(DefaultDistinctNameService.SERVICE_NAME);
        DefaultDistinctNameService service = (DefaultDistinctNameService) existingDefaultLocalEJBReceiverServiceController.getValue();
        if (!defaultDistinctName.isDefined()) {
            service.setDefaultDistinctName(null);
        } else {
            service.setDefaultDistinctName(defaultDistinctName.asString());
        }
    }

}
