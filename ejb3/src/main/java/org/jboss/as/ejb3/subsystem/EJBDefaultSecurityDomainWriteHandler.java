/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.concurrent.atomic.AtomicReference;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

/**
 * Write handler for default security domain attribute of EJB3 subsystem
 *
 * @author Jaikiran Pai
 */
class EJBDefaultSecurityDomainWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition attributeDefinition;
    private final AtomicReference<String> defaultSecurityDomainName;

    EJBDefaultSecurityDomainWriteHandler(final AttributeDefinition attributeDefinition, final AtomicReference<String> defaultSecurityDomainName) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
        this.defaultSecurityDomainName = defaultSecurityDomainName;
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateDefaultSecurityDomainDeploymentProcessor(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateDefaultSecurityDomainDeploymentProcessor(context, restored);
    }

    private void updateDefaultSecurityDomainDeploymentProcessor(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode defaultSecurityDomainModelNode = this.attributeDefinition.resolveModelAttribute(context, model);
        final String defaultSecurityDomainName = defaultSecurityDomainModelNode.isDefined() ? defaultSecurityDomainModelNode.asString() : null;
        this.defaultSecurityDomainName.set(defaultSecurityDomainName);
    }

}
