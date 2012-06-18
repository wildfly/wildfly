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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.DISCOVERY_GROUP_NAME;
import static org.jboss.as.messaging.CommonAttributes.LOCAL;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_TX;
import static org.jboss.as.messaging.CommonAttributes.MAX_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.MIN_POOL_SIZE;
import static org.jboss.as.messaging.CommonAttributes.NONE;
import static org.jboss.as.messaging.CommonAttributes.NO_TX;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION;
import static org.jboss.as.messaging.CommonAttributes.XA_TX;
import static org.jboss.as.messaging.jms.PooledConnectionFactoryAttribute.getDefinitions;

import java.util.ArrayList;
import java.util.List;

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/13/11
 *         Time: 1:42 PM
 */
public class PooledConnectionFactoryAdd extends AbstractAddStepHandler {

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        for(final AttributeDefinition attribute : getDefinitions(JMSServices.POOLED_CONNECTION_FACTORY_ATTRS)) {
            final String attrName = attribute.getName();
            if(subModel.has(attrName)) {
                operation.get(attrName).set(subModel.get(attrName));
            }
        }

        return operation;
    }

    public static final PooledConnectionFactoryAdd INSTANCE = new PooledConnectionFactoryAdd();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        for(final AttributeDefinition attribute : getDefinitions(JMSServices.POOLED_CONNECTION_FACTORY_ATTRS)) {
            attribute.validateAndSet(operation, model);
        }

    }

    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> newControllers) throws OperationFailedException {

        ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        final ModelNode resolvedModel = model.clone();
        for(final AttributeDefinition attribute : getDefinitions(JMSServices.POOLED_CONNECTION_FACTORY_ATTRS)) {
            resolvedModel.get(attribute.getName()).set(attribute.resolveModelAttribute(context, resolvedModel ));
        }

        // We validated that jndiName part of the model in populateModel
        // TODO we only use a single jndi name here but the xsd indicates support for many
        final String jndiName = resolvedModel.get(CommonAttributes.ENTRIES.getName()).asList().get(0).asString();

        final int minPoolSize = resolvedModel.get(MIN_POOL_SIZE.getName()).asInt();
        final int maxPoolSize = resolvedModel.get(MAX_POOL_SIZE.getName()).asInt();

        final String txSupport;
        if(resolvedModel.hasDefined(TRANSACTION)) {
            String txType = resolvedModel.get(TRANSACTION).asString();
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

        List<PooledConnectionFactoryConfigProperties> adapterParams = getAdapterParams(resolvedModel, context);

        final ServiceName hqServiceName = MessagingServices.getHornetQServiceName(address);
        ServiceName hornetQResourceAdapterService = JMSServices.getPooledConnectionFactoryBaseServiceName(hqServiceName).append(name);
        PooledConnectionFactoryService resourceAdapterService = new PooledConnectionFactoryService(name, connectors, discoveryGroupName, adapterParams, jndiName, txSupport, minPoolSize, maxPoolSize);
        ServiceBuilder serviceBuilder = serviceTarget
                .addService(hornetQResourceAdapterService, resourceAdapterService)
                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, resourceAdapterService.getTransactionManager())
                .addDependency(hqServiceName, HornetQServer.class, resourceAdapterService.getHornetQService())
                .addDependency(JMSServices.getJmsManagerBaseServiceName(hqServiceName))
                .addListener(verificationHandler);

        newControllers.add(serviceBuilder.setInitialMode(Mode.ACTIVE).install());
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
        if(model.hasDefined(DISCOVERY_GROUP_NAME.getName())) {
            return model.get(DISCOVERY_GROUP_NAME.getName()).asString();
        }
        return null;
    }
    static List<PooledConnectionFactoryConfigProperties> getAdapterParams(ModelNode model, OperationContext context) throws OperationFailedException {
        List<PooledConnectionFactoryConfigProperties> configs = new ArrayList<PooledConnectionFactoryConfigProperties>();
        for (PooledConnectionFactoryAttribute nodeAttribute : JMSServices.POOLED_CONNECTION_FACTORY_ATTRS)
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
