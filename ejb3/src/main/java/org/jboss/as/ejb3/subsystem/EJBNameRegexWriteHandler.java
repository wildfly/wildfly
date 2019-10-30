/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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
