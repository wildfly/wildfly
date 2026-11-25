/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.jboss.as.controller.ModuleIdentifierUtil.parseCanonicalModuleIdentifier;

import java.util.Properties;
import org.apache.activemq.artemis.jms.bridge.ConnectionFactoryFactory;
import org.apache.activemq.artemis.jms.bridge.DestinationFactory;
import org.apache.activemq.artemis.jms.bridge.JMSBridge;
import org.apache.activemq.artemis.jms.bridge.QualityOfServiceMode;
import org.apache.activemq.artemis.jms.bridge.impl.JMSBridgeImpl;
import org.apache.activemq.artemis.jms.bridge.impl.JNDIConnectionFactoryFactory;
import org.apache.activemq.artemis.jms.bridge.impl.JNDIDestinationFactory;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 *
 * @author Emmanuel Hugonnet (c) 2020 Red Hat, Inc.
 */
public class JMSBridgeFactory {

    public static JMSBridge createJMSBridge(OperationContext context, ModelNode model) throws OperationFailedException {
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

        final String sourceUsername = JMSBridgeDefinition.SOURCE_USER.resolveModelAttribute(context, model).asStringOrNull();
        final String sourcePassword = JMSBridgeDefinition.SOURCE_PASSWORD.resolveModelAttribute(context, model).asStringOrNull();
        final String targetUsername = JMSBridgeDefinition.TARGET_USER.resolveModelAttribute(context, model).asStringOrNull();
        final String targetPassword = JMSBridgeDefinition.TARGET_PASSWORD.resolveModelAttribute(context, model).asStringOrNull();

        final String selector = CommonAttributes.SELECTOR.resolveModelAttribute(context, model).asStringOrNull();
        final long failureRetryInterval = JMSBridgeDefinition.FAILURE_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final int maxRetries = JMSBridgeDefinition.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        final QualityOfServiceMode qosMode = QualityOfServiceMode.valueOf( JMSBridgeDefinition.QUALITY_OF_SERVICE.resolveModelAttribute(context, model).asString());
        final int maxBatchSize = JMSBridgeDefinition.MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        final long maxBatchTime = JMSBridgeDefinition.MAX_BATCH_TIME.resolveModelAttribute(context, model).asLong();
        final String subName =  JMSBridgeDefinition.SUBSCRIPTION_NAME.resolveModelAttribute(context, model).asStringOrNull();
        final String clientID = JMSBridgeDefinition.CLIENT_ID.resolveModelAttribute(context, model).asStringOrNull();
        final boolean addMessageIDInHeader = JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER.resolveModelAttribute(context, model).asBoolean();

        final String moduleName = JMSBridgeDefinition.MODULE.resolveModelAttribute(context, model).asStringOrNull();

        final ClassLoader oldTccl= WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            // if a module is specified, use it to instantiate the JMSBridge to ensure its ExecutorService
            // will use the correct class loader to execute its threads
            if (moduleName != null) {
                org.jboss.modules.Module module = org.jboss.modules.Module.getCallerModuleLoader().loadModule(parseCanonicalModuleIdentifier(moduleName));
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
            }
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
                    addMessageIDInHeader).setBridgeName(context.getCurrentAddressValue());
        } catch (ModuleNotFoundException e) {
            throw MessagingLogger.ROOT_LOGGER.moduleNotFound(moduleName, e.getMessage(), e);
        } catch (ModuleLoadException e) {
            throw MessagingLogger.ROOT_LOGGER.unableToLoadModule(moduleName, e);
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(oldTccl);
        }
    }

    public static Properties resolveContextProperties(AttributeDefinition attribute, OperationContext context, ModelNode model) throws OperationFailedException {
        final ModelNode contextModel = attribute.resolveModelAttribute(context, model);
        final Properties contextProperties = new Properties();
        if (contextModel.isDefined()) {
            for (Property property : contextModel.asPropertyList()) {
                contextProperties.put(property.getName(), property.getValue().asString());
            }
        }
        return contextProperties;
    }
}
