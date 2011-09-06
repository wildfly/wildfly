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

package org.jboss.as.messaging;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.operations.ServerWriteAttributeOperationHandler;
import org.jboss.dmr.ModelNode;

/**
 * Write attribute handler for attributes that update a discovery group resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DiscoveryGroupWriteAttributeHandler extends ServerWriteAttributeOperationHandler {

    public static final DiscoveryGroupWriteAttributeHandler INSTANCE = new DiscoveryGroupWriteAttributeHandler();

    private final Map<String, AttributeDefinition> attributes = new HashMap<String, AttributeDefinition>();
    private DiscoveryGroupWriteAttributeHandler() {
        for (AttributeDefinition attr : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
            attributes.put(attr.getName(), attr);
        }
    }

    public void registerAttributes(final ManagementResourceRegistration registry) {
        final EnumSet<AttributeAccess.Flag> flags = EnumSet.of(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        for (AttributeDefinition attr : CommonAttributes.DISCOVERY_GROUP_ATTRIBUTES) {
            registry.registerReadWriteAttribute(attr.getName(), null, this, flags);
        }
    }

    @Override
    protected void modelChanged(final OperationContext context, final ModelNode operation, final String attributeName,
                                final ModelNode newValue, final ModelNode currentValue) throws OperationFailedException {
        super.modelChanged(context, operation, attributeName, newValue, currentValue);
        context.addStep(new OperationStepHandler() {
            @Override
            public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                final AttributeDefinition attr = attributes.get(attributeName);
                final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
                if(attr.hasAlternative(resource.getModel())) {
                    context.setRollbackOnly();
                    throw new OperationFailedException(new ModelNode().set(String.format("Alternative attribute of (%s) is already defined."), attributeName));
                }
            }
        }, OperationContext.Stage.VERIFY);
    }

    @Override
    protected void validateValue(String name, ModelNode value) throws OperationFailedException {
        AttributeDefinition attr = attributes.get(name);
        attr.getValidator().validateParameter(name, value);
    }

    @Override
    protected void validateResolvedValue(String name, ModelNode value) throws OperationFailedException {
        // no-op, as we are not going to apply this value until the server is reloaded, so allow the
        // any system property to be set between now and then
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode newValue, ModelNode currentValue) throws OperationFailedException {
        return true;
    }

}
