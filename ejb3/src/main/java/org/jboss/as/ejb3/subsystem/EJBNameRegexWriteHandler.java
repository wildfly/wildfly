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
class EJBNameRegexWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final EJBNameRegexWriteHandler INSTANCE = new EJBNameRegexWriteHandler(EJB3SubsystemRootResourceDefinition.ALLOW_EJB_NAME_REGEX);

    private final AttributeDefinition attributeDefinition;

    private EJBNameRegexWriteHandler(final AttributeDefinition attributeDefinition) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateRegexAllowed(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateRegexAllowed(context, restored);
    }

    void updateRegexAllowed(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode allowRegex = this.attributeDefinition.resolveModelAttribute(context, model);
        final ServiceRegistry registry = context.getServiceRegistry(true);

        final ServiceController<?> ejbNameServiceController = registry.getService(EjbNameRegexService.SERVICE_NAME);
        EjbNameRegexService service = (EjbNameRegexService) ejbNameServiceController.getValue();
        if (!allowRegex.isDefined()) {
            service.setEjbNameRegexAllowed(false);
        } else {
            service.setEjbNameRegexAllowed(allowRegex.asBoolean());
        }
    }

}
