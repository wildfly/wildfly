/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.jsf.deployment.JSFAnnotationProcessor;
import org.jboss.as.jsf.deployment.JSFBeanValidationFactoryProcessor;
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
 * The Jakarta Server Faces subsystem add update handler.
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

                processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JSF_ANNOTATIONS, new JSFAnnotationProcessor());
                if (context.hasOptionalCapability("org.wildfly.bean-validation", JSFResourceDefinition.FACES_CAPABILITY.getName(), null)) {
                    processorTarget.addDeploymentProcessor(JSFExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JSF_VALIDATOR_FACTORY, new JSFBeanValidationFactoryProcessor());
                }
            }
        }, OperationContext.Stage.RUNTIME);
    }
}
