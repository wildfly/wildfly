/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.deployment.processors.EJBDefaultSecurityDomainProcessor;
import org.jboss.dmr.ModelNode;

/**
 * Write handler for default security domain attribute of EJB3 subsystem
 *
 * @author Jaikiran Pai
 */
class EJBDefaultSecurityDomainWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private final AttributeDefinition attributeDefinition;
    private final EJBDefaultSecurityDomainProcessor ejbDefaultSecurityDomainProcessor;

    EJBDefaultSecurityDomainWriteHandler(final AttributeDefinition attributeDefinition, final EJBDefaultSecurityDomainProcessor ejbDefaultSecurityDomainProcessor) {
        super(attributeDefinition);
        this.attributeDefinition = attributeDefinition;
        this.ejbDefaultSecurityDomainProcessor = ejbDefaultSecurityDomainProcessor;
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

        if (this.ejbDefaultSecurityDomainProcessor == null) {
            return;
        }
        final ModelNode defaultSecurityDomainModelNode = this.attributeDefinition.resolveModelAttribute(context, model);
        final String defaultSecurityDomainName = defaultSecurityDomainModelNode.isDefined() ? defaultSecurityDomainModelNode.asString() : null;
        this.ejbDefaultSecurityDomainProcessor.setDefaultSecurityDomainName(defaultSecurityDomainName);
    }

}
