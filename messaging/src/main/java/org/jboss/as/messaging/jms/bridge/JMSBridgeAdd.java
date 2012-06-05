/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.operations.common.Util.getOperation;

import java.util.List;
import java.util.Properties;

import org.hornetq.jms.bridge.ConnectionFactoryFactory;
import org.hornetq.jms.bridge.DestinationFactory;
import org.hornetq.jms.bridge.JMSBridge;
import org.hornetq.jms.bridge.QualityOfServiceMode;
import org.hornetq.jms.bridge.impl.JMSBridgeImpl;
import org.hornetq.jms.bridge.impl.JNDIConnectionFactoryFactory;
import org.hornetq.jms.bridge.impl.JNDIDestinationFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.as.messaging.jms.JMSServices;
import org.jboss.as.messaging.jms.SelectorAttribute;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * JMS Bridge add update.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeAdd extends AbstractAddStepHandler {
    public static final JMSBridgeAdd INSTANCE = new JMSBridgeAdd();

    /**
     * Create an "add" operation using the existing model
     */
    public static ModelNode getAddOperation(final ModelNode address, ModelNode subModel) {
        return getOperation(ADD, address, subModel);
    }

    private JMSBridgeAdd() {
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attributeDefinition : JMSBridgeDefinition.JMS_BRIDGE_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
        for (final AttributeDefinition attributeDefinition : JMSBridgeDefinition.JMS_TARGET_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
        for (final AttributeDefinition attributeDefinition : JMSBridgeDefinition.JMS_SOURCE_ATTRIBUTES) {
            attributeDefinition.validateAndSet(operation, model);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
            final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers)
                    throws OperationFailedException {
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));

                String moduleName = resolveAttribute(JMSBridgeDefinition.MODULE, context, model);
                final JMSBridge bridge = createJMSBridge(context, model);

                final String bridgeName = address.getLastElement().getValue();
                final JMSBridgeService bridgeService = new JMSBridgeService(moduleName, bridgeName, bridge);
                final ServiceName bridgeServiceName = MessagingServices.getJMSBridgeServiceName(bridgeName);

                final ServiceBuilder<JMSBridge> jmsBridgeServiceBuilder = context.getServiceTarget().addService(bridgeServiceName, bridgeService)
                        .addListener(verificationHandler)
                        .addDependency(TxnServices.JBOSS_TXN_TRANSACTION_MANAGER)
                        .setInitialMode(Mode.ACTIVE);
                if (dependsOnHornetQServer(context, model)) {
                    // add a dependency to the JMS Manager instead of HornetQ service since it is this service
                    // that effectively start HornetQ
                    jmsBridgeServiceBuilder.addDependency((JMSServices.getJmsManagerBaseServiceName(MessagingServices.getHornetQServiceName("default"))));
                }
                newControllers.add(jmsBridgeServiceBuilder.install());

                context.completeStep();
            }
        }, OperationContext.Stage.RUNTIME);
    }

    private boolean dependsOnHornetQServer(OperationContext context, ModelNode model) throws OperationFailedException {
        final Properties sourceContextProperties = resolveContextProperties(JMSBridgeDefinition.SOURCE_CONTEXT, context, model);
        final Properties targetContextProperties = resolveContextProperties(JMSBridgeDefinition.TARGET_CONTEXT, context, model);

        // if either the source or target context properties are null, this means that the JMS resources will be looked up
        // from the local HornetQ server.
        return (sourceContextProperties == null || targetContextProperties == null);
    }

    private JMSBridge createJMSBridge(OperationContext context, ModelNode model) throws OperationFailedException {
        final Properties sourceContextProperties = resolveContextProperties(JMSBridgeDefinition.SOURCE_CONTEXT, context, model);
        final String sourceConnectionFactoryName = JMSBridgeDefinition.SOURCE_CONNECTION_FACTORY.resolveModelAttribute(context, model).asString();
        final ConnectionFactoryFactory sourceCff = new JNDIConnectionFactoryFactory(sourceContextProperties , sourceConnectionFactoryName);
        final String sourceDestinationName = JMSBridgeDefinition.SOURCE_DESTINATION.resolveModelAttribute(context, model).asString();
        final DestinationFactory sourceDestinationFactory = new JNDIDestinationFactory(sourceContextProperties, sourceDestinationName);

        final Properties targetContextProperties = resolveContextProperties(JMSBridgeDefinition.TARGET_CONTEXT, context, model);
        final String targetConnectionFactoryName = JMSBridgeDefinition.TARGET_CONNECTION_FACTORY.resolveModelAttribute(context, model).asString();
        final ConnectionFactoryFactory targetCff = new JNDIConnectionFactoryFactory(targetContextProperties, targetConnectionFactoryName);
        final String targetDestinationName = JMSBridgeDefinition.TARGET_DESTINATION.resolveModelAttribute(context, model).asString();
        final DestinationFactory targetDestinationFactory = new JNDIDestinationFactory(targetContextProperties, targetDestinationName);

        final String sourceUsername = resolveAttribute(JMSBridgeDefinition.SOURCE_USER, context, model);
        final String sourcePassword = resolveAttribute(JMSBridgeDefinition.SOURCE_PASSWORD, context, model);
        final String targetUsername = resolveAttribute(JMSBridgeDefinition.TARGET_USER, context, model);
        final String targetPassword = resolveAttribute(JMSBridgeDefinition.TARGET_PASSWORD, context, model);

        final String selector = resolveAttribute(SelectorAttribute.SELECTOR, context, model);
        final long failureRetryInterval = JMSBridgeDefinition.FAILURE_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final int maxRetries = JMSBridgeDefinition.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        final QualityOfServiceMode qosMode = QualityOfServiceMode.valueOf( JMSBridgeDefinition.QUALITY_OF_SERVICE.resolveModelAttribute(context, model).asString());
        final int maxBatchSize = JMSBridgeDefinition.MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        final long maxBatchTime = JMSBridgeDefinition.MAX_BATCH_TIME.resolveModelAttribute(context, model).asLong();
        final String subName =  resolveAttribute(JMSBridgeDefinition.SUBSCRIPTION_NAME, context, model);
        final String clientID = resolveAttribute(JMSBridgeDefinition.CLIENT_ID, context, model);
        final boolean addMessageIDInHeader = JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER.resolveModelAttribute(context, model).asBoolean();

        return new JMSBridgeImpl(sourceCff,
                targetCff,
                sourceDestinationFactory,
                targetDestinationFactory,
                sourceUsername,
                sourcePassword,
                targetUsername,
                targetPassword,
                selector,
                failureRetryInterval,
                maxRetries,
                qosMode,
                maxBatchSize,
                maxBatchTime,
                subName,
                clientID,
                addMessageIDInHeader);
    }

    private Properties resolveContextProperties(AttributeDefinition attribute, OperationContext context, ModelNode model) throws OperationFailedException {
        final ModelNode contextModel = attribute.resolveModelAttribute(context, model);
        if (!contextModel.isDefined()) {
            return null;
        }

        final Properties contextProperties = new Properties();
        for (Property property : contextModel.asPropertyList()) {
            contextProperties.put(property.getName(), property.getValue().asString());
        }
        return contextProperties;
    }

    /**
     * Return null if the resolved attribute is not defined
     */
    private String resolveAttribute(SimpleAttributeDefinition attr, OperationContext context, ModelNode model) throws OperationFailedException {
        final ModelNode node = attr.resolveModelAttribute(context, model);
        return node.isDefined() ? node.asString() : null;
    }
}
