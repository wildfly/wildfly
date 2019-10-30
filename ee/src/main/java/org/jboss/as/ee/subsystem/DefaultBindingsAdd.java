/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

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
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_DEFAULT_BINDINGS_CONFIG, defaultBindingsConfigurationProcessor);
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
