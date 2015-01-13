/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.weld;

import static org.jboss.as.weld.WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.as.weld.deployment.CdiAnnotationProcessor;
import org.jboss.as.weld.deployment.processors.BeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.BeanDefiningAnnotationProcessor;
import org.jboss.as.weld.deployment.processors.BeansXmlProcessor;
import org.jboss.as.weld.deployment.processors.CdiBeanValidationFactoryProcessor;
import org.jboss.as.weld.deployment.processors.ExternalBeanArchiveProcessor;
import org.jboss.as.weld.deployment.processors.WebIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldBeanManagerServiceProcessor;
import org.jboss.as.weld.deployment.processors.WeldComponentIntegrationProcessor;
import org.jboss.as.weld.deployment.processors.WeldConfigurationProcessor;
import org.jboss.as.weld.deployment.processors.WeldDependencyProcessor;
import org.jboss.as.weld.deployment.processors.WeldDeploymentProcessor;
import org.jboss.as.weld.deployment.processors.WeldImplicitDeploymentProcessor;
import org.jboss.as.weld.deployment.processors.WeldPortableExtensionProcessor;
import org.jboss.as.weld.services.TCCLSingletonService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * The weld subsystem add update handler.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
class WeldSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final WeldSubsystemAdd INSTANCE = new WeldSubsystemAdd();

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        WeldResourceDefinition.REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE.validateAndSet(operation, model);
        WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(final OperationContext context, ModelNode operation,Resource resource) throws OperationFailedException {

        final ModelNode model = resource.getModel();
        final boolean requireBeanDescriptor = REQUIRE_BEAN_DESCRIPTOR_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean();
        final boolean nonPortableMode = WeldResourceDefinition.NON_PORTABLE_MODE_ATTRIBUTE.resolveModelAttribute(context, model).asBoolean();

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_WELD, new JBossAllXmlParserRegisteringProcessor<>(WeldJBossAllParser.ROOT_ELEMENT, WeldJBossAllConfiguration.ATTACHMENT_KEY, WeldJBossAllParser.INSTANCE));
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WELD_CONFIGURATION, new WeldConfigurationProcessor(requireBeanDescriptor, nonPortableMode));
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_CDI_ANNOTATIONS, new CdiAnnotationProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_CDI_BEAN_DEFINING_ANNOTATIONS, new BeanDefiningAnnotationProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WELD_DEPLOYMENT, new BeansXmlProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WELD_IMPLICIT_DEPLOYMENT_DETECTION, new WeldImplicitDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_WELD, new WeldDependencyProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_WEB_INTEGRATION, new WebIntegrationProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_BEAN_ARCHIVE, new BeanArchiveProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_EXTERNAL_BEAN_ARCHIVE, new ExternalBeanArchiveProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_PORTABLE_EXTENSIONS, new WeldPortableExtensionProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_WELD_COMPONENT_INTEGRATION, new WeldComponentIntegrationProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WELD_DEPLOYMENT, new WeldDeploymentProcessor(checkJtsEnabled(context)));
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_CDI_VALIDATOR_FACTORY, new CdiBeanValidationFactoryProcessor());
                processorTarget.addDeploymentProcessor(WeldExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WELD_BEAN_MANAGER, new WeldBeanManagerServiceProcessor());
            }
        }, OperationContext.Stage.RUNTIME);

        TCCLSingletonService singleton = new TCCLSingletonService();
        context.getServiceTarget().addService(TCCLSingletonService.SERVICE_NAME, singleton).setInitialMode(
                Mode.ON_DEMAND).install();
    }

    // Synchronization objects created by iiop ejb beans require wrapping by JTSSychronizationWrapper to work correctly
    // (WFLY-3538). This hack is used obtain jts configuration in order to avoid doing this in non-jts environments when it is
    // not necessary.
    private boolean checkJtsEnabled(final OperationContext context) {
        final ModelNode jtsNode = context.readResourceFromRoot(PathAddress.pathAddress("subsystem", "transactions"), false)
                .getModel().get("jts");
        return jtsNode.isDefined() ? jtsNode.asBoolean() : false;
    }
}
