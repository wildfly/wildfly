/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.subsystem;

import static org.jboss.as.server.deployment.Phase.STRUCTURE_EE_DEFAULT_BINDINGS_CONFIG;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.component.deployers.DefaultBindingsConfigurationProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * @author Eduardo Martins
 */
public class DefaultBindingsAdd extends AbstractBoottimeAddStepHandler {

    private final DefaultBindingsConfigurationProcessor defaultBindingsConfigurationProcessor;

    public DefaultBindingsAdd(DefaultBindingsConfigurationProcessor defaultBindingsConfigurationProcessor) {
        super(DefaultBindingsResourceDefinition.ATTRIBUTES);
        this.defaultBindingsConfigurationProcessor = defaultBindingsConfigurationProcessor;
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();
        if(model.hasDefined(DefaultBindingsResourceDefinition.CONTEXT_SERVICE)) {
            final String contextService = DefaultBindingsResourceDefinition.CONTEXT_SERVICE_AD.resolveModelAttribute(context, model).asString();
            defaultBindingsConfigurationProcessor.setContextService(contextService);
        }
        if(model.hasDefined(DefaultBindingsResourceDefinition.DATASOURCE)) {
            final String dataSource = DefaultBindingsResourceDefinition.DATASOURCE_AD.resolveModelAttribute(context, model).asString();
            defaultBindingsConfigurationProcessor.setDataSource(dataSource);
        }
        if(model.hasDefined(DefaultBindingsResourceDefinition.JMS_CONNECTION_FACTORY)) {
            final String jmsConnectionFactory = DefaultBindingsResourceDefinition.JMS_CONNECTION_FACTORY_AD.resolveModelAttribute(context, model).asString();
            defaultBindingsConfigurationProcessor.setJmsConnectionFactory(jmsConnectionFactory);
        }
        if(model.hasDefined(DefaultBindingsResourceDefinition.MANAGED_EXECUTOR_SERVICE)) {
            final String managedExecutorService = DefaultBindingsResourceDefinition.MANAGED_EXECUTOR_SERVICE_AD.resolveModelAttribute(context, model).asString();
            defaultBindingsConfigurationProcessor.setManagedExecutorService(managedExecutorService);
        }
        if(model.hasDefined(DefaultBindingsResourceDefinition.MANAGED_SCHEDULED_EXECUTOR_SERVICE)) {
            final String managedScheduledExecutorService = DefaultBindingsResourceDefinition.MANAGED_SCHEDULED_EXECUTOR_SERVICE_AD.resolveModelAttribute(context, model).asString();
            defaultBindingsConfigurationProcessor.setManagedScheduledExecutorService(managedScheduledExecutorService);
        }
        if(model.hasDefined(DefaultBindingsResourceDefinition.MANAGED_THREAD_FACTORY)) {
            final String managedThreadFactory = DefaultBindingsResourceDefinition.MANAGED_THREAD_FACTORY_AD.resolveModelAttribute(context, model).asString();
            defaultBindingsConfigurationProcessor.setManagedThreadFactory(managedThreadFactory);
        }

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, STRUCTURE_EE_DEFAULT_BINDINGS_CONFIG, defaultBindingsConfigurationProcessor);
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
