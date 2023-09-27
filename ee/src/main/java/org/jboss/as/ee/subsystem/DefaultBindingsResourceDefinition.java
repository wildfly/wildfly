/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.ee.component.deployers.DefaultBindingsConfigurationProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * The resource definition wrt EE default bindings configuration.
 * @author Eduardo Martins
 */
public class DefaultBindingsResourceDefinition extends SimpleResourceDefinition {

    public static final String CONTEXT_SERVICE = "context-service";
    public static final String DATASOURCE = "datasource";
    public static final String JMS_CONNECTION_FACTORY = "jms-connection-factory";
    public static final String MANAGED_EXECUTOR_SERVICE = "managed-executor-service";
    public static final String MANAGED_SCHEDULED_EXECUTOR_SERVICE = "managed-scheduled-executor-service";
    public static final String MANAGED_THREAD_FACTORY = "managed-thread-factory";

    public static final SimpleAttributeDefinition CONTEXT_SERVICE_AD =
            new SimpleAttributeDefinitionBuilder(CONTEXT_SERVICE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    public static final SimpleAttributeDefinition DATASOURCE_AD =
            new SimpleAttributeDefinitionBuilder(DATASOURCE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    public static final SimpleAttributeDefinition JMS_CONNECTION_FACTORY_AD =
            new SimpleAttributeDefinitionBuilder(JMS_CONNECTION_FACTORY, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    public static final SimpleAttributeDefinition MANAGED_EXECUTOR_SERVICE_AD =
            new SimpleAttributeDefinitionBuilder(MANAGED_EXECUTOR_SERVICE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    public static final SimpleAttributeDefinition MANAGED_SCHEDULED_EXECUTOR_SERVICE_AD =
            new SimpleAttributeDefinitionBuilder(MANAGED_SCHEDULED_EXECUTOR_SERVICE, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    public static final SimpleAttributeDefinition MANAGED_THREAD_FACTORY_AD =
            new SimpleAttributeDefinitionBuilder(MANAGED_THREAD_FACTORY, ModelType.STRING, true)
                    .setAllowExpression(true)
                    .build();

    static final SimpleAttributeDefinition[] ATTRIBUTES = { CONTEXT_SERVICE_AD, DATASOURCE_AD, JMS_CONNECTION_FACTORY_AD, MANAGED_EXECUTOR_SERVICE_AD, MANAGED_SCHEDULED_EXECUTOR_SERVICE_AD, MANAGED_THREAD_FACTORY_AD };

    private final DefaultBindingsConfigurationProcessor defaultBindingsConfigurationProcessor;

    public DefaultBindingsResourceDefinition(DefaultBindingsConfigurationProcessor defaultBindingsConfigurationProcessor) {
        super(EESubsystemModel.DEFAULT_BINDINGS_PATH, EeExtension.getResourceDescriptionResolver(EESubsystemModel.DEFAULT_BINDINGS), new DefaultBindingsAdd(defaultBindingsConfigurationProcessor), ReloadRequiredRemoveStepHandler.INSTANCE);
        this.defaultBindingsConfigurationProcessor = defaultBindingsConfigurationProcessor;
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        OperationStepHandler writeHandler = new WriteAttributeHandler();
        for (AttributeDefinition attr : ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    private class WriteAttributeHandler extends AbstractWriteAttributeHandler<Void> {

        private WriteAttributeHandler() {
            super(DefaultBindingsResourceDefinition.ATTRIBUTES);
        }

        @Override
        protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                               ModelNode newValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
            applyUpdateToDeploymentUnitProcessor(attributeName, newValue);
            return false;
        }

        @Override
        protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                             ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
            applyUpdateToDeploymentUnitProcessor(attributeName, valueToRestore);
        }

        private void applyUpdateToDeploymentUnitProcessor(final String attributeName, final ModelNode newValue) throws OperationFailedException {
            final String attributeValue = newValue.isDefined() ? newValue.asString() : null;
            if (DefaultBindingsResourceDefinition.CONTEXT_SERVICE.equals(attributeName)) {
                defaultBindingsConfigurationProcessor.setContextService(attributeValue);
            } else if (DefaultBindingsResourceDefinition.DATASOURCE.equals(attributeName)) {
                defaultBindingsConfigurationProcessor.setDataSource(attributeValue);
            } else if (DefaultBindingsResourceDefinition.JMS_CONNECTION_FACTORY.equals(attributeName)) {
                defaultBindingsConfigurationProcessor.setJmsConnectionFactory(attributeValue);
            } else if (DefaultBindingsResourceDefinition.MANAGED_EXECUTOR_SERVICE.equals(attributeName)) {
                defaultBindingsConfigurationProcessor.setManagedExecutorService(attributeValue);
            } else if (DefaultBindingsResourceDefinition.MANAGED_SCHEDULED_EXECUTOR_SERVICE.equals(attributeName)) {
                defaultBindingsConfigurationProcessor.setManagedScheduledExecutorService(attributeValue);
            } else if (DefaultBindingsResourceDefinition.MANAGED_THREAD_FACTORY.equals(attributeName)) {
                defaultBindingsConfigurationProcessor.setManagedThreadFactory(attributeValue);
            }
        }
    }
}
