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

import static org.jboss.as.controller.OperationContext.Stage.MODEL;
import static org.jboss.as.messaging.MessagingMessages.MESSAGES;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
/**
 * Write attribute handler for attributes that update a broadcast group resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BroadcastGroupWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final BroadcastGroupWriteAttributeHandler INSTANCE = new BroadcastGroupWriteAttributeHandler();

    private BroadcastGroupWriteAttributeHandler() {
        super(BroadcastGroupDefinition.ATTRIBUTES);
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        context.addStep(new AlternativeAttributeCheckHandler(BroadcastGroupDefinition.ATTRIBUTES), MODEL);

        super.execute(context, operation);
    }

    @Override
    protected void finishModelStage(final OperationContext context,final ModelNode operation,final String attributeName,final ModelNode newValue,
            final ModelNode oldValue,final Resource model) throws OperationFailedException {
            if(attributeName.equals(BroadcastGroupDefinition.CONNECTOR_REFS.getName())){
                this.validateConnectorsUpdate(context, operation, newValue, oldValue,model);
            }
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
    }

    private void validateConnectorsUpdate(OperationContext context, ModelNode operation, ModelNode newValue,
            ModelNode oldValue, Resource model) throws OperationFailedException {
        final Set<String> availableConnectors =  getAvailableConnectors(context,operation);
        final List<ModelNode> operationAddress = operation.get(ModelDescriptionConstants.ADDRESS).asList();
        final String broadCastGroup = operationAddress.get(operationAddress.size()-1).get(CommonAttributes.BROADCAST_GROUP).asString();
        for(ModelNode connectorRef:newValue.asList()){
            final String connectorName = connectorRef.asString();
            if(!availableConnectors.contains(connectorName)){
                throw MESSAGES.wrongConnectorRefInBroadCastGroup(broadCastGroup,connectorName,availableConnectors);
            }
        }
    }

    private Set<String> getAvailableConnectors(final OperationContext context,final ModelNode operation) throws OperationFailedException{
        PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        PathAddress hornetqServer = MessagingServices.getHornetQServerPathAddress(address);
        Resource hornetQServerResource = context.readResourceFromRoot(hornetqServer);
        Set<String> availableConnectors = new HashSet<String>();
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
        availableConnectors.addAll(hornetQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
        return availableConnectors;
    }
}
