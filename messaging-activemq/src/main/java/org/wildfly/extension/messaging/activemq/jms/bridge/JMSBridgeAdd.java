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

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.jboss.as.server.Services.requireServerExecutor;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.apache.activemq.artemis.jms.bridge.ConnectionFactoryFactory;
import org.apache.activemq.artemis.jms.bridge.DestinationFactory;
import org.apache.activemq.artemis.jms.bridge.JMSBridge;
import org.apache.activemq.artemis.jms.bridge.QualityOfServiceMode;
import org.apache.activemq.artemis.jms.bridge.impl.JMSBridgeImpl;
import org.apache.activemq.artemis.jms.bridge.impl.JNDIConnectionFactoryFactory;
import org.apache.activemq.artemis.jms.bridge.impl.JNDIDestinationFactory;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleNotFoundException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq.CommonAttributes;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.extension.messaging.activemq.logging.MessagingLogger;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * JMS Bridge add update.
 *
 * @author Jeff Mesnil (c) 2012 Red Hat Inc.
 */
public class JMSBridgeAdd extends AbstractAddStepHandler {
    public static final JMSBridgeAdd INSTANCE = new JMSBridgeAdd();

    private JMSBridgeAdd() {
        super(JMSBridgeDefinition.ATTRIBUTES);
    }

    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws  OperationFailedException {
        super.populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();
        handleCredentialReferenceUpdate(context, model.get(JMSBridgeDefinition.SOURCE_CREDENTIAL_REFERENCE.getName()), JMSBridgeDefinition.SOURCE_CREDENTIAL_REFERENCE.getName());
        handleCredentialReferenceUpdate(context, model.get(JMSBridgeDefinition.TARGET_CREDENTIAL_REFERENCE.getName()), JMSBridgeDefinition.TARGET_CREDENTIAL_REFERENCE.getName());
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model)
                    throws OperationFailedException {
        context.addStep(new OperationStepHandler() {

            @Override
            public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                final PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));

                String moduleName = resolveAttribute(JMSBridgeDefinition.MODULE, context, model);
                final JMSBridge bridge = createJMSBridge(context, model);

                final String bridgeName = address.getLastElement().getValue();
                final ServiceName bridgeServiceName = MessagingServices.getJMSBridgeServiceName(bridgeName);

                final ServiceBuilder jmsBridgeServiceBuilder = context.getServiceTarget().addService(bridgeServiceName);
                jmsBridgeServiceBuilder.requires(context.getCapabilityServiceName(MessagingServices.LOCAL_TRANSACTION_PROVIDER_CAPABILITY, null));
                jmsBridgeServiceBuilder.setInitialMode(Mode.ACTIVE);
                Supplier<ExecutorService> executorSupplier = requireServerExecutor(jmsBridgeServiceBuilder);
                if (dependsOnLocalResources(context, model, JMSBridgeDefinition.SOURCE_CONTEXT)) {
                    addDependencyForJNDIResource(jmsBridgeServiceBuilder, model, context, JMSBridgeDefinition.SOURCE_CONNECTION_FACTORY);
                    addDependencyForJNDIResource(jmsBridgeServiceBuilder, model, context, JMSBridgeDefinition.SOURCE_DESTINATION);
                }
                if (dependsOnLocalResources(context, model, JMSBridgeDefinition.TARGET_CONTEXT)) {
                    addDependencyForJNDIResource(jmsBridgeServiceBuilder, model, context, JMSBridgeDefinition.TARGET_CONNECTION_FACTORY);
                    addDependencyForJNDIResource(jmsBridgeServiceBuilder, model, context, JMSBridgeDefinition.TARGET_DESTINATION);
                }
                // add a dependency to the Artemis thread pool so that if either the source or target JMS broker
                // corresponds to a local Artemis server, the pool will be cleaned up after the JMS bridge is stopped.
                jmsBridgeServiceBuilder.requires(MessagingServices.ACTIVEMQ_CLIENT_THREAD_POOL);
                // adding credential source supplier which will later resolve password from CredentialStore using credential-reference
                final JMSBridgeService bridgeService = new JMSBridgeService(moduleName, bridgeName, bridge, executorSupplier,
                        getCredentialStoreReference(JMSBridgeDefinition.SOURCE_CREDENTIAL_REFERENCE, context, model, jmsBridgeServiceBuilder),
                        getCredentialStoreReference(JMSBridgeDefinition.TARGET_CREDENTIAL_REFERENCE, context, model, jmsBridgeServiceBuilder));
                jmsBridgeServiceBuilder.setInstance(bridgeService);
                jmsBridgeServiceBuilder.install();

                context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            }

        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
        rollbackCredentialStoreUpdate(JMSBridgeDefinition.SOURCE_CREDENTIAL_REFERENCE, context, resource);
        rollbackCredentialStoreUpdate(JMSBridgeDefinition.TARGET_CREDENTIAL_REFERENCE, context, resource);
    }

    private boolean dependsOnLocalResources(OperationContext context, ModelNode model, AttributeDefinition attr) throws OperationFailedException {
        // if either the source or target context attribute is resolved to a null or empty Properties, this means that the JMS resources will be
        // looked up from the local ActiveMQ server.
        return resolveContextProperties(attr, context, model).isEmpty();
    }

    private void addDependencyForJNDIResource(final ServiceBuilder builder, final ModelNode model, final OperationContext context,
            final AttributeDefinition attribute) throws OperationFailedException {
        String jndiName = attribute.resolveModelAttribute(context, model).asString();
        builder.requires(ContextNames.bindInfoFor(jndiName).getBinderServiceName());
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

        final String selector = resolveAttribute(CommonAttributes.SELECTOR, context, model);
        final long failureRetryInterval = JMSBridgeDefinition.FAILURE_RETRY_INTERVAL.resolveModelAttribute(context, model).asLong();
        final int maxRetries = JMSBridgeDefinition.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        final QualityOfServiceMode qosMode = QualityOfServiceMode.valueOf( JMSBridgeDefinition.QUALITY_OF_SERVICE.resolveModelAttribute(context, model).asString());
        final int maxBatchSize = JMSBridgeDefinition.MAX_BATCH_SIZE.resolveModelAttribute(context, model).asInt();
        final long maxBatchTime = JMSBridgeDefinition.MAX_BATCH_TIME.resolveModelAttribute(context, model).asLong();
        final String subName =  resolveAttribute(JMSBridgeDefinition.SUBSCRIPTION_NAME, context, model);
        final String clientID = resolveAttribute(JMSBridgeDefinition.CLIENT_ID, context, model);
        final boolean addMessageIDInHeader = JMSBridgeDefinition.ADD_MESSAGE_ID_IN_HEADER.resolveModelAttribute(context, model).asBoolean();

        final String moduleName = resolveAttribute(JMSBridgeDefinition.MODULE, context, model);

        final ClassLoader oldTccl= WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            // if a module is specified, use it to instantiate the JMSBridge to ensure its ExecutorService
            // will use the correct class loader to execute its threads
            if (moduleName != null) {
                Module module = Module.getCallerModuleLoader().loadModule(ModuleIdentifier.fromString(moduleName));
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

    private Properties resolveContextProperties(AttributeDefinition attribute, OperationContext context, ModelNode model) throws OperationFailedException {
        final ModelNode contextModel = attribute.resolveModelAttribute(context, model);
        final Properties contextProperties = new Properties();
        if (contextModel.isDefined()) {
            for (Property property : contextModel.asPropertyList()) {
                contextProperties.put(property.getName(), property.getValue().asString());
            }
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

    private static ExceptionSupplier<CredentialSource, Exception> getCredentialStoreReference(ObjectTypeAttributeDefinition credentialReferenceAttributeDefinition, OperationContext context, ModelNode model, ServiceBuilder<?> serviceBuilder, String... modelFilter) throws OperationFailedException {
        if (model.hasDefined(credentialReferenceAttributeDefinition.getName())) {
            ModelNode filteredModelNode = model;
            if (modelFilter != null && modelFilter.length > 0) {
                for (String path : modelFilter) {
                    if (filteredModelNode.get(path).isDefined()) {
                        filteredModelNode = filteredModelNode.get(path);
                    } else {
                        break;
                    }
                }
            }
            ModelNode value = credentialReferenceAttributeDefinition.resolveModelAttribute(context, filteredModelNode);
            if (value.isDefined()) {
               return CredentialReference.getCredentialSourceSupplier(context, credentialReferenceAttributeDefinition, filteredModelNode, serviceBuilder);
            }
        }
        return null;
    }
}
