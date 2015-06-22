/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.messaging;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.messaging.deployment.CDIDeploymentProcessor;
import org.jboss.as.messaging.deployment.DefaultJMSConnectionFactoryBindingProcessor;
import org.jboss.as.messaging.deployment.DefaultJMSConnectionFactoryResourceReferenceProcessor;
import org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionAnnotationProcessor;
import org.jboss.as.messaging.deployment.JMSConnectionFactoryDefinitionDescriptorProcessor;
import org.jboss.as.messaging.deployment.JMSDestinationDefinitionAnnotationProcessor;
import org.jboss.as.messaging.deployment.JMSDestinationDefinitionDescriptorProcessor;
import org.jboss.as.messaging.deployment.MessagingDependencyProcessor;
import org.jboss.as.messaging.deployment.MessagingXmlInstallDeploymentUnitProcessor;
import org.jboss.as.messaging.deployment.MessagingXmlParsingDeploymentUnitProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * Add handler for the messaging subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
class MessagingSubsystemAdd extends AbstractBoottimeAddStepHandler {

    public static final MessagingSubsystemAdd INSTANCE = new MessagingSubsystemAdd();

    private MessagingSubsystemAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        model.setEmptyObject();
    }

    @Override
    protected void performBoottime(final OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                // keep the statements ordered by phase + priority
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_JMS_CONNECTION_FACTORY_RESOURCE_INJECTION, new DefaultJMSConnectionFactoryResourceReferenceProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_JMS_DESTINATION, new JMSDestinationDefinitionAnnotationProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_RESOURCE_DEF_ANNOTATION_JMS_CONNECTION_FACTORY, new JMSConnectionFactoryDefinitionAnnotationProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_MESSAGING_XML_RESOURCES, new MessagingXmlParsingDeploymentUnitProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JMS, new MessagingDependencyProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JMS_CDI_EXTENSIONS, new CDIDeploymentProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_JMS_CONNECTION_FACTORY, new JMSConnectionFactoryDefinitionDescriptorProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_RESOURCE_DEF_XML_JMS_DESTINATION, new JMSDestinationDefinitionDescriptorProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEFAULT_BINDINGS_JMS_CONNECTION_FACTORY, new DefaultJMSConnectionFactoryBindingProcessor());
                processorTarget.addDeploymentProcessor(MessagingExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_MESSAGING_XML_RESOURCES, new MessagingXmlInstallDeploymentUnitProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

}
