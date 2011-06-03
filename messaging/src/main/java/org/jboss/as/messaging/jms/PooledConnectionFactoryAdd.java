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

import org.hornetq.core.server.HornetQServer;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.messaging.CommonAttributes;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.threads.Element;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import java.util.ArrayList;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.LOCAL;
import static org.jboss.as.messaging.CommonAttributes.NONE;
import static org.jboss.as.messaging.jms.JMSServices.CONNECTION_FACTORY_ATTRS;
import static org.jboss.as.messaging.CommonAttributes.NO_TX;
import static org.jboss.as.messaging.CommonAttributes.XA_TX;
import static org.jboss.as.messaging.CommonAttributes.TRANSACTION;
import static org.jboss.as.messaging.CommonAttributes.LOCAL_TX;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/13/11
 *         Time: 1:42 PM
 */
public class PooledConnectionFactoryAdd implements ModelAddOperationHandler {

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {

        final ModelNode operation = new ModelNode();
        operation.get(OP).set(ADD);
        operation.get(OP_ADDR).set(address);

        for(final JMSServices.NodeAttribute attribute : JMSServices.POOLED_CONNECTION_FACTORY_ATTRS) {
            final String attrName = attribute.getName();
            if(subModel.has(attrName)) {
                operation.get(attrName).set(subModel.get(attrName));
            }
        }

        return operation;
    }

    public static final PooledConnectionFactoryAdd INSTANCE = new PooledConnectionFactoryAdd();

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {

        ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        final ModelNode compensatingOperation = Util.getResourceRemoveOperation(opAddr);

        final String jndiName;
        if(operation.hasDefined(CommonAttributes.ENTRIES)) {
            List<ModelNode> entries = operation.get(CommonAttributes.ENTRIES).asList();
            if(entries.size() == 0) {
                throw new OperationFailedException("at least 1 jndi entry should be provided", operation);
            }
            jndiName = entries.get(0).asString();
        } else {
            throw new OperationFailedException("at least 1 jndi entry should be provided", operation);
        }

        final String txSupport;
        if(operation.hasDefined(TRANSACTION)) {
            String txType = operation.get(TRANSACTION).asString();
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
        final ModelNode subModel = context.getSubModel();
        for(final JMSServices.NodeAttribute attribute : CONNECTION_FACTORY_ATTRS) {
            final String attrName = attribute.getName();
            if(operation.hasDefined(attrName)) {
                subModel.get(attrName).set(operation.get(attrName));
            }
        }


        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {

                public void execute(RuntimeTaskContext context) throws OperationFailedException {

                    try {

                        ServiceTarget serviceTarget = context.getServiceTarget();

                        List<String> connectors = getConnectors(operation);

                        List<PooledConnectionFactoryConfigProperties> adapterParams = getAdapterParams(operation);

                        ServiceName hornetQResourceAdapterService = MessagingServices.POOLED_CONNECTION_FACTORY_BASE.append(name);
                        PooledConnectionFactoryService resourceAdapterService = new PooledConnectionFactoryService(name, connectors, adapterParams, jndiName, txSupport);
                        ServiceBuilder serviceBuilder = serviceTarget
                                .addService(hornetQResourceAdapterService, resourceAdapterService)
                                .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, resourceAdapterService.getTransactionManager())
                                .addDependency(MessagingServices.JBOSS_MESSAGING, HornetQServer.class, resourceAdapterService.getHornetQService());
                        serviceBuilder.setInitialMode(Mode.ACTIVE).install();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                        resultHandler.handleCancellation();
                    }

                    resultHandler.handleResultComplete();
                }
            });
        }

        return new BasicOperationResult(compensatingOperation);
    }

    static List<String> getConnectors(final ModelNode operation) {
        List<String> connectorNames = new ArrayList<String>();
        if (operation.hasDefined(CONNECTOR)) {
            for (String connectorName : operation.get(CONNECTOR).keys()) {
                connectorNames.add(connectorName);
            }
        }
        return connectorNames;
    }

    static List<PooledConnectionFactoryConfigProperties> getAdapterParams(ModelNode operation) {
        List<PooledConnectionFactoryConfigProperties> configs = new ArrayList<PooledConnectionFactoryConfigProperties>();
        for (JMSServices.PooledCFAttribute nodeAttribute : JMSServices.POOLED_CONNECTION_FACTORY_METHOD_ATTRS)
        {
            if(operation.hasDefined(nodeAttribute.getName())) {
                String value = operation.get(nodeAttribute.getName()).asString();
                configs.add(new PooledConnectionFactoryConfigProperties(nodeAttribute.getMethodName(), value, nodeAttribute.getClassType()));
            }
        }
        return configs;
    }
}
