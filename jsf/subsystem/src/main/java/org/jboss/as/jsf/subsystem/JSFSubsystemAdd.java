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

package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jsf.deployment.JSFAnnotationProcessor;
import org.jboss.as.jsf.deployment.JSFBeanValidationFactoryProcessor;
import org.jboss.as.jsf.deployment.JSFCdiExtensionDeploymentProcessor;
import org.jboss.as.jsf.deployment.JSFComponentProcessor;
import org.jboss.as.jsf.deployment.JSFDependencyProcessor;
import org.jboss.as.jsf.deployment.JSFMetadataProcessor;
import org.jboss.as.jsf.deployment.JSFSharedTldsProcessor;
import org.jboss.as.jsf.deployment.JSFVersionProcessor;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;

/**
 * The JSF subsystem add update handler.
 *
 * @author Stuart Douglas
 */
class JSFSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final JSFSubsystemAdd INSTANCE = new JSFSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        JSFResourceDefinition.DEFAULT_JSF_IMPL_SLOT.validateAndSet(operation, model);
        JSFResourceDefinition.DISALLOW_DOCTYPE_DECL.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, final ModelNode model) throws OperationFailedException {
        final String defaultJSFSlot = JSFResourceDefinition.DEFAULT_JSF_IMPL_SLOT.resolveModelAttribute(context, model).asString();
        final Boolean disallowDoctypeDecl = JSFResourceDefinition.DISALLOW_DOCTYPE_DECL.resolveModelAttribute(context, model).asBooleanOrNull();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JSF_VERSION, new JSFVersionProcessor(defaultJSFSlot));
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JSF_SHARED_TLDS, new JSFSharedTldsProcessor());
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JSF_METADATA, new JSFMetadataProcessor(disallowDoctypeDecl));
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_JSF, new JSFDependencyProcessor());
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JSF_MANAGED_BEANS, new JSFComponentProcessor());
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JSF_CDI_EXTENSIONS, new JSFCdiExtensionDeploymentProcessor());
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JSF_ANNOTATIONS, new JSFAnnotationProcessor());
                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JSF_VALIDATOR_FACTORY, new JSFBeanValidationFactoryProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
