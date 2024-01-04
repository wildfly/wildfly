/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms.bridge;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;
import static org.jboss.as.server.Services.requireServerExecutor;
import static org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeFactory.createJMSBridge;
import static org.wildfly.extension.messaging.activemq.jms.bridge.JMSBridgeFactory.resolveContextProperties;

import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.extension.messaging.activemq.MessagingServices;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * Jakarta Messaging Bridge add update.
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

                String moduleName = JMSBridgeDefinition.MODULE.resolveModelAttribute(context, model).asStringOrNull();
                final String bridgeName = context.getCurrentAddressValue();
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
                // add a dependency to the Artemis thread pool so that if either the source or target Jakarta Messaging broker
                // corresponds to a local Artemis server, the pool will be cleaned up after the Jakarta Messaging bridge is stopped.
                jmsBridgeServiceBuilder.requires(MessagingServices.ACTIVEMQ_CLIENT_THREAD_POOL);
                // adding credential source supplier which will later resolve password from CredentialStore using credential-reference
                final JMSBridgeService bridgeService = new JMSBridgeService(moduleName, bridgeName, createJMSBridge(context, model), executorSupplier,
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
        // if either the source or target context attribute is resolved to a null or empty Properties, this means that the Jakarta Messaging resources will be
        // looked up from the local ActiveMQ server.
        return resolveContextProperties(attr, context, model).isEmpty();
    }

    private void addDependencyForJNDIResource(final ServiceBuilder builder, final ModelNode model, final OperationContext context,
            final AttributeDefinition attribute) throws OperationFailedException {
        String jndiName = attribute.resolveModelAttribute(context, model).asString();
        builder.requires(ContextNames.bindInfoFor(jndiName).getBinderServiceName());
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
