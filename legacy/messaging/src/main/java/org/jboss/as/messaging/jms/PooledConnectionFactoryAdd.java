/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.JGROUPS_CHANNEL;
import static org.jboss.as.messaging.CommonAttributes.LOCAL;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_TX;
import static org.jboss.as.messaging.CommonAttributes.NONE;
import static org.jboss.as.messaging.CommonAttributes.NO_TX;
import static org.jboss.as.messaging.CommonAttributes.XA_TX;
import static org.jboss.as.messaging.jms.ConnectionFactoryAttribute.getDefinitions;

import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.messaging.AlternativeAttributeCheckHandler;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.ConnectionFactoryAttributes.Common;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/13/11
 *         Time: 1:42 PM
 */
public class PooledConnectionFactoryAdd extends AbstractAddStepHandler {

    public static final PooledConnectionFactoryAdd INSTANCE = new PooledConnectionFactoryAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        AlternativeAttributeCheckHandler.checkAlternatives(operation, Common.CONNECTOR.getName(), Common.DISCOVERY_GROUP_NAME.getName(), false);

        for(final AttributeDefinition attribute : getDefinitions(PooledConnectionFactoryDefinition.ATTRIBUTES)) {
            attribute.validateAndSet(operation, model);
        }
    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        final ModelNode resolvedModel = model.clone();
        for(final AttributeDefinition attribute : getDefinitions(PooledConnectionFactoryDefinition.ATTRIBUTES)) {
            resolvedModel.get(attribute.getName()).set(attribute.resolveModelAttribute(context, resolvedModel ));
        }

        // We validated that jndiName part of the model in populateModel
        final List<String> jndiNames = new ArrayList<String>();
        for (ModelNode node : resolvedModel.get(Common.ENTRIES.getName()).asList()) {
            jndiNames.add(node.asString());
        }

        final int minPoolSize = resolvedModel.get(ConnectionFactoryAttributes.Pooled.MIN_POOL_SIZE.getName()).asInt();
        final int maxPoolSize = resolvedModel.get(ConnectionFactoryAttributes.Pooled.MAX_POOL_SIZE.getName()).asInt();

        final String txSupport;
        if(resolvedModel.hasDefined(ConnectionFactoryAttributes.Pooled.TRANSACTION.getName())) {
            String txType = resolvedModel.get(ConnectionFactoryAttributes.Pooled.TRANSACTION.getName()).asString();
            if(LOCAL.equals(txType)) {
                txSupport = LOCAL_TX;
            } else if (NONE.equals(txType)) {
                 txSupport = NO_TX;
            } else {
                txSupport = XA_TX;
            }
        } else {
            txSupport = XA_TX;
        }

        ServiceTarget serviceTarget = context.getServiceTarget();

        List<String> connectors = getConnectors(resolvedModel);

        String discoveryGroupName = getDiscoveryGroup(resolvedModel);
        String jgroupsChannelName = null;
        if (discoveryGroupName != null) {
            Resource dgResource = context.readResourceFromRoot(MessagingServices.getHornetQServerPathAddress(address).append(CommonAttributes.DISCOVERY_GROUP, discoveryGroupName));
            ModelNode dgModel = dgResource.getModel();
            jgroupsChannelName = JGROUPS_CHANNEL.resolveModelAttribute(context, dgModel).asString();
        }

        List<PooledConnectionFactoryConfigProperties> adapterParams = getAdapterParams(resolvedModel, context);

        final PathAddress hqServiceAddress = MessagingServices.getHornetQServerPathAddress(address);

        PooledConnectionFactoryService.installService(serviceTarget, name, hqServiceAddress.getLastElement().getValue(), connectors, discoveryGroupName, jgroupsChannelName,
                adapterParams, jndiNames, txSupport, minPoolSize, maxPoolSize);
    }

    static List<String> getConnectors(final ModelNode model) {
        List<String> connectorNames = new ArrayList<String>();
        if (model.hasDefined(CONNECTOR)) {
            for (String connectorName : model.get(CONNECTOR).keys()) {
                connectorNames.add(connectorName);
            }
        }
        return connectorNames;
    }

    static String getDiscoveryGroup(final ModelNode model) {
        if(model.hasDefined(Common.DISCOVERY_GROUP_NAME.getName())) {
            return model.get(Common.DISCOVERY_GROUP_NAME.getName()).asString();
        }
        return null;
    }
    static List<PooledConnectionFactoryConfigProperties> getAdapterParams(ModelNode model, OperationContext context) throws OperationFailedException {
        List<PooledConnectionFactoryConfigProperties> configs = new ArrayList<PooledConnectionFactoryConfigProperties>();
        for (ConnectionFactoryAttribute nodeAttribute : PooledConnectionFactoryDefinition.ATTRIBUTES)
        {
            if (!nodeAttribute.isResourceAdapterProperty())
                continue;

            AttributeDefinition definition = nodeAttribute.getDefinition();
            ModelNode node = definition.resolveModelAttribute(context, model);
            if (node.isDefined()) {
                String value = node.asString();
                configs.add(new PooledConnectionFactoryConfigProperties(nodeAttribute.getPropertyName(), value, nodeAttribute.getClassType()));
            }
        }
        return configs;
    }
}
