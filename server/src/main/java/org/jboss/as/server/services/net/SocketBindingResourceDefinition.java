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

package org.jboss.as.server.services.net;

import org.jboss.as.controller.ControllerMessages;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for a server socket binding resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SocketBindingResourceDefinition extends AbstractSocketBindingResourceDefinition {

    public static final SocketBindingResourceDefinition INSTANCE = new SocketBindingResourceDefinition();

    private SocketBindingResourceDefinition() {
        super(BindingAddHandler.INSTANCE, BindingRemoveHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // Metrics
        resourceRegistration.registerMetric(BindingMetricHandlers.BoundHandler.ATTRIBUTE_DEFINITION, BindingMetricHandlers.BoundHandler.INSTANCE);
        resourceRegistration.registerMetric(BindingMetricHandlers.BoundAddressHandler.ATTRIBUTE_DEFINITION, BindingMetricHandlers.BoundAddressHandler.INSTANCE);
        resourceRegistration.registerMetric(BindingMetricHandlers.BoundPortHandler.ATTRIBUTE_DEFINITION, BindingMetricHandlers.BoundPortHandler.INSTANCE);
    }

    @Override
    protected OperationStepHandler getInterfaceWriteAttributeHandler() {
        return BindingInterfaceHandler.INSTANCE;
    }

    @Override
    protected OperationStepHandler getPortWriteAttributeHandler() {
        return BindingPortHandler.INSTANCE;
    }

    @Override
    protected OperationStepHandler getFixedPortWriteAttributeHandler() {
        return BindingFixedPortHandler.INSTANCE;
    }

    @Override
    protected OperationStepHandler getMulticastAddressWriteAttributeHandler() {
        return BindingMulticastAddressHandler.INSTANCE;
    }

    @Override
    protected OperationStepHandler getMulticastPortWriteAttributeHandler() {
        return BindingMulticastPortHandler.INSTANCE;
    }

    @Override
    protected OperationStepHandler getClientMappingsWriteAttributeHandler() {
        return ClientMappingsHandler.INSTANCE;
    }

    static void validateInterfaceReference(final OperationContext context, final ModelNode binding) throws OperationFailedException {

        ModelNode interfaceNode = binding.get(INTERFACE.getName());
        if (interfaceNode.getType() == ModelType.STRING) { // ignore UNDEFINED and EXPRESSION
            String interfaceName = interfaceNode.asString();
            PathAddress interfaceAddress = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.INTERFACE, interfaceName));
            try {
                context.readResourceFromRoot(interfaceAddress, false);
            } catch (RuntimeException e) {
                throw ControllerMessages.MESSAGES.nonexistentInterface(interfaceName, INTERFACE.getName());
            }
        }

    }
}
