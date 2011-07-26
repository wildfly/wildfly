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

import java.util.Locale;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Removes a divert.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class DivertRemove extends AbstractRemoveStepHandler implements DescriptionProvider {

    public static final DivertRemove INSTANCE = new DivertRemove();

    private DivertRemove() {
        super();
    }

    @Override
    protected void performRemove(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
        if (hqService != null && hqService.getState() == ServiceController.State.UP) {

            HornetQServer server = HornetQServer.class.cast(hqService.getValue());
            try {
                server.getHornetQServerControl().destroyDivert(name);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // TODO should this be an OFE instead?
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String name = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();
        final ModelNode routingNode = CommonAttributes.ROUTING_NAME.validateResolvedOperation(model);
        final String routingName = routingNode.isDefined() ? routingNode.asString() : null;
        final String address = CommonAttributes.DIVERT_ADDRESS.validateResolvedOperation(model).asString();
        final String forwardingAddress = CommonAttributes.FORWARDING_ADDRESS.validateResolvedOperation(model).asString();
        final boolean exclusive = CommonAttributes.EXCLUSIVE.validateResolvedOperation(model).asBoolean();
        final ModelNode filterNode = CommonAttributes.FILTER.validateResolvedOperation(model);
        final String filter = filterNode.isDefined() ? filterNode.asString() : null;
        final ModelNode transformerNode =  CommonAttributes.TRANSFORMER_CLASS_NAME.validateResolvedOperation(model);
        final String transformerClassName = transformerNode.isDefined() ? transformerNode.asString() : null;

        final ServiceRegistry registry = context.getServiceRegistry(true);
        final ServiceController<?> hqService = registry.getService(MessagingServices.JBOSS_MESSAGING);
        if (hqService != null && hqService.getState() == ServiceController.State.UP) {

            HornetQServer server = HornetQServer.class.cast(hqService.getValue());
            try {
                server.getHornetQServerControl().createDivert(name, routingName, address, forwardingAddress, exclusive, filter, transformerClassName);
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                // TODO should this be an OFE instead?
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return MessagingDescriptions.getDivertRemove(locale);
    }
}
