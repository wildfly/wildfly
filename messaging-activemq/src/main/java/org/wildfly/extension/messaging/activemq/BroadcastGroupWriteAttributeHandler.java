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

package org.wildfly.extension.messaging.activemq;

import static org.wildfly.extension.messaging.activemq.CommonAttributes.CONNECTORS;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.BROADCAST_GROUP_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.JGROUPS_BROADCAST_GROUP_PATH;
import static org.wildfly.extension.messaging.activemq.MessagingExtension.SOCKET_BROADCAST_GROUP_PATH;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
/**
 * Write attribute handler for attributes that update a broadcast group resource.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BroadcastGroupWriteAttributeHandler extends ReloadRequiredWriteAttributeHandler {

    public static final BroadcastGroupWriteAttributeHandler INSTANCE = new BroadcastGroupWriteAttributeHandler(BROADCAST_GROUP_PATH, BroadcastGroupDefinition.ATTRIBUTES);
    public static final BroadcastGroupWriteAttributeHandler JGROUP_INSTANCE = new BroadcastGroupWriteAttributeHandler(JGROUPS_BROADCAST_GROUP_PATH, JGroupsBroadcastGroupDefinition.ATTRIBUTES);
    public static final BroadcastGroupWriteAttributeHandler SOCKET_INSTANCE = new BroadcastGroupWriteAttributeHandler(SOCKET_BROADCAST_GROUP_PATH, JGroupsBroadcastGroupDefinition.ATTRIBUTES);

    private final PathElement path;

    private BroadcastGroupWriteAttributeHandler(final PathElement path, final AttributeDefinition... definitions) {
        super(definitions);
        this.path = path;
    }

    @Override
    protected void finishModelStage(final OperationContext context,final ModelNode operation,final String attributeName,final ModelNode newValue,
            final ModelNode oldValue,final Resource model) throws OperationFailedException {
            if(attributeName.equals(CONNECTORS)){
                validateConnectors(context, operation, newValue);
            }
        super.finishModelStage(context, operation, attributeName, newValue, oldValue, model);
    }

    void validateConnectors(OperationContext context, ModelNode operation, ModelNode connectorRefs) throws OperationFailedException {
        final Set<String> availableConnectors =  getAvailableConnectors(context,operation);
        final List<ModelNode> operationAddress = operation.get(ModelDescriptionConstants.OP_ADDR).asList();
        final String broadCastGroup = operationAddress.get(operationAddress.size()-1).get(path.getKey()).asString();
        if(connectorRefs.isDefined()) {
            for(ModelNode connectorRef:connectorRefs.asList()){
                final String connectorName = connectorRef.asString();
                if(!availableConnectors.contains(connectorName)){
                    throw MessagingLogger.ROOT_LOGGER.wrongConnectorRefInBroadCastGroup(broadCastGroup, connectorName, availableConnectors);
                }
            }
        }
    }

    // FIXME use capabilities & requirements
    private static Set<String> getAvailableConnectors(final OperationContext context,final ModelNode operation) throws OperationFailedException{
        PathAddress address = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR));
        PathAddress active = MessagingServices.getActiveMQServerPathAddress(address);
        Set<String> availableConnectors = new HashSet<>();

        Resource subsystemResource = context.readResourceFromRoot(active.getParent(), false);
        availableConnectors.addAll(subsystemResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));

        Resource activeMQServerResource = context.readResourceFromRoot(active, false);
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.HTTP_CONNECTOR));
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.IN_VM_CONNECTOR));
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.REMOTE_CONNECTOR));
        availableConnectors.addAll(activeMQServerResource.getChildrenNames(CommonAttributes.CONNECTOR));
        return availableConnectors;
    }
}
