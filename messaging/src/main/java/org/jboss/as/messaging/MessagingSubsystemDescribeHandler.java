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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PATH;

import java.util.Locale;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.jms.ConnectionFactoryAdd;
import org.jboss.as.messaging.jms.JMSQueueAdd;
import org.jboss.as.messaging.jms.JMSTopicAdd;
import org.jboss.as.messaging.jms.PooledConnectionFactoryAdd;
import org.jboss.as.messaging.jms.bridge.JMSBridgeAdd;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 */
class MessagingSubsystemDescribeHandler implements OperationStepHandler, DescriptionProvider {

    static final String[] TRANSPORT = new String[] {CommonAttributes.ACCEPTOR, CommonAttributes.REMOTE_ACCEPTOR, CommonAttributes.IN_VM_ACCEPTOR,
                                CommonAttributes.CONNECTOR, CommonAttributes.REMOTE_CONNECTOR, CommonAttributes.IN_VM_CONNECTOR};

    static final MessagingSubsystemDescribeHandler INSTANCE = new MessagingSubsystemDescribeHandler();

    /** {@inheritDoc} */
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        final ModelNode subModel = Resource.Tools.readModel(resource);

        final ModelNode subsystemAdd = new ModelNode();
        PathAddress rootAddress = PathAddress.pathAddress(PathAddress.pathAddress(operation.require(OP_ADDR)).getLastElement());
        subsystemAdd.get(OP).set(ADD);
        subsystemAdd.get(OP_ADDR).set(rootAddress.toModelNode());

        final ModelNode result = context.getResult();
        result.add(subsystemAdd);


        if (subModel.hasDefined(CommonAttributes.HORNETQ_SERVER)) {
            for (Property prop : subModel.get(CommonAttributes.HORNETQ_SERVER).asPropertyList()) {
                ModelNode serverAdd = new ModelNode();
                serverAdd.get(OP).set(ADD);
                PathAddress serverAddress = rootAddress.append(PathElement.pathElement(CommonAttributes.HORNETQ_SERVER, prop.getName()));
                serverAdd.get(OP_ADDR).set(serverAddress.toModelNode());
                addHornetQServer(prop.getValue(), serverAdd, serverAddress, result);
            }
        }

        if (subModel.hasDefined(CommonAttributes.JMS_BRIDGE)) {
            for (Property prop : subModel.get(CommonAttributes.JMS_BRIDGE).asPropertyList()) {
                PathAddress jmsBridgeAddress = rootAddress.append(PathElement.pathElement(CommonAttributes.JMS_BRIDGE, prop.getName()));
                result.add(JMSBridgeAdd.getAddOperation(jmsBridgeAddress.toModelNode(), prop.getValue()));
            }
        }

        context.completeStep();
    }

    private void addHornetQServer(final ModelNode subModel, final ModelNode serverAdd, final PathAddress rootAddress, final ModelNode result) {
        //
        for(final AttributeDefinition attribute : CommonAttributes.SIMPLE_ROOT_RESOURCE_ATTRIBUTES) {
            String attrName = attribute.getName();
            if(subModel.hasDefined(attrName)) {
                serverAdd.get(attrName).set(subModel.get(attrName));
            }
        }

        result.add(serverAdd);

        // acceptors / connectors
        for(final String transport : TRANSPORT) {
            if(subModel.hasDefined(transport)) {
                for(final Property property : subModel.get(transport).asPropertyList()) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(transport, property.getName());
                    result.add(TransportConfigOperationHandlers.createAddOperation(address, property.getValue()));
                }
            }
        }

        // the paths
        if(subModel.hasDefined(PATH)) {
            final ModelNode paths = subModel.get(PATH);
            for(final String pathName : CommonAttributes.PATHS) {
                if(paths.hasDefined(pathName)) {
                    final ModelNode address = rootAddress.toModelNode();
                    address.add(PATH, pathName);
                    result.add(MessagingPathHandlers.createAddOperation(address, paths.get(pathName)));
                }
            }
        }

        if(subModel.hasDefined(CommonAttributes.ADDRESS_SETTING)) {
            for(final Property property : subModel.get(CommonAttributes.ADDRESS_SETTING).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.ADDRESS_SETTING, property.getName());
                result.add(AddressSettingAdd.createAddOperation(address, property.getValue()));
            }
        }

        if (subModel.hasDefined(CommonAttributes.BROADCAST_GROUP)) {
            for(final Property property : subModel.get(CommonAttributes.BROADCAST_GROUP).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.BROADCAST_GROUP, property.getName());
                result.add(BroadcastGroupAdd.getAddOperation(address, property.getValue()));
            }
        }

        if (subModel.hasDefined(CommonAttributes.DISCOVERY_GROUP)) {
            for(final Property property : subModel.get(CommonAttributes.DISCOVERY_GROUP).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.DISCOVERY_GROUP, property.getName());
                result.add(DiscoveryGroupAdd.getAddOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.DIVERT)) {
            for(final Property property : subModel.get(CommonAttributes.DIVERT).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.DIVERT, property.getName());
                result.add(DivertAdd.getAddOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.QUEUE)) {
            for(final Property property : subModel.get(CommonAttributes.QUEUE).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.QUEUE, property.getName());
                result.add(QueueAdd.getAddOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.BRIDGE)) {
            for(final Property property : subModel.get(CommonAttributes.BRIDGE).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.BRIDGE, property.getName());
                result.add(BridgeAdd.getAddOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.CLUSTER_CONNECTION)) {
            for(final Property property : subModel.get(CommonAttributes.CLUSTER_CONNECTION).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CLUSTER_CONNECTION, property.getName());
                result.add(ClusterConnectionAdd.getAddOperation(address, property.getValue()));
            }
        }

        if (subModel.hasDefined(CommonAttributes.GROUPING_HANDLER)) {
            Property property = subModel.get(CommonAttributes.GROUPING_HANDLER).asProperty();
            final ModelNode address = rootAddress.toModelNode();
            address.add(CommonAttributes.GROUPING_HANDLER, property.getName());
            result.add(GroupingHandlerAdd.getAddOperation(address, property.getValue()));
        }

        if (subModel.hasDefined(CommonAttributes.CONNECTOR_SERVICE)) {
            for(final Property property : subModel.get(CommonAttributes.CONNECTOR_SERVICE).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CONNECTOR_SERVICE, property.getName());
                final ModelNode csNode = property.getValue();
                result.add(ConnectorServiceAdd.getAddOperation(address, csNode));
                if (csNode.hasDefined(CommonAttributes.PARAM)) {
                    for(final Property param : csNode.get(CommonAttributes.PARAM).asPropertyList()) {
                        final ModelNode paramAddress = address.clone().add(CommonAttributes.PARAM, param.getName());
                        result.add(ConnectorServiceParamAdd.getAddOperation(paramAddress, param.getValue()));
                    }
                }
            }
        }

        if(subModel.hasDefined(CommonAttributes.CONNECTION_FACTORY)) {
            for(final Property property : subModel.get(CommonAttributes.CONNECTION_FACTORY).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.CONNECTION_FACTORY, property.getName());
                result.add(ConnectionFactoryAdd.getAddOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.POOLED_CONNECTION_FACTORY)) {
            for(final Property property : subModel.get(CommonAttributes.POOLED_CONNECTION_FACTORY).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.POOLED_CONNECTION_FACTORY, property.getName());
                result.add(PooledConnectionFactoryAdd.getAddOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.JMS_QUEUE)) {
            for(final Property property : subModel.get(CommonAttributes.JMS_QUEUE).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.JMS_QUEUE, property.getName());
                result.add(JMSQueueAdd.getOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.JMS_TOPIC)) {
            for(final Property property : subModel.get(CommonAttributes.JMS_TOPIC).asPropertyList()) {
                final ModelNode address = rootAddress.toModelNode();
                address.add(CommonAttributes.JMS_TOPIC, property.getName());
                result.add(JMSTopicAdd.getOperation(address, property.getValue()));
            }
        }

        if(subModel.hasDefined(CommonAttributes.SECURITY_SETTING)) {
            final ModelNode securitySettings = subModel.get(CommonAttributes.SECURITY_SETTING);
            for(final Property setting : securitySettings.asPropertyList()) {
                final ModelNode settingAddress = rootAddress.toModelNode();
                settingAddress.add(CommonAttributes.SECURITY_SETTING, setting.getName());
                result.add(SecuritySettingAdd.createAddOperation(settingAddress, setting.getValue()));
                final ModelNode securitySetting = setting.getValue();
                if(securitySetting.hasDefined(CommonAttributes.ROLE)) {
                    for(final Property role : securitySetting.get(CommonAttributes.ROLE).asPropertyList()) {
                        final ModelNode address = settingAddress.clone();
                        address.add(CommonAttributes.ROLE, role.getName());
                        result.add(SecurityRoleAdd.createAddOperation(address, role.getValue()));
                    }
                }
            }
        }
    }

    public ModelNode getModelDescription(Locale locale) {
        // Private operation; no real description needed
        return new ModelNode();
    }

}
