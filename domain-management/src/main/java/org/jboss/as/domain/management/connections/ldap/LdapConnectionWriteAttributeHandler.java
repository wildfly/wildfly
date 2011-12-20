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

package org.jboss.as.domain.management.connections.ldap;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Handler for updating attributes of ldap management connections.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class LdapConnectionWriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

    public LdapConnectionWriteAttributeHandler() {
        super(LdapConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS);
    }

    void registerAttributes(final ManagementResourceRegistration registration) {
        for (AttributeDefinition attr : LdapConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            registration.registerReadWriteAttribute(attr, null, this);
        }
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName,
                                           final ModelNode resolvedValue, final ModelNode currentValue,
                                           final HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateLdapConnectionService(context, operation, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName,
                                         final ModelNode valueToRestore, final ModelNode valueToRevert,
                                         final Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateLdapConnectionService(context, operation, restored);
    }

    private void updateLdapConnectionService(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        ServiceName svcName = LdapConnectionManagerService.BASE_SERVICE_NAME.append(name);
        ServiceRegistry registry = context.getServiceRegistry(true);
        ServiceController<?> controller = registry.getService(svcName);
        if (controller != null) {
            // Just set the new values on the existing service
            final ModelNode resolvedConfig = LdapConnectionAddHandler.createResolvedLdapConfiguration(context, model);
            LdapConnectionManagerService service = LdapConnectionManagerService.class.cast(controller.getValue());
            service.setResolvedConfiguration(resolvedConfig);
        } else {
            // Nothing to do
        }
    }
}
