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

package org.jboss.as.web;

import java.util.List;
import java.util.Locale;

import javax.management.MBeanServer;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.AbstractPathService;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.as.web.deployment.EarContextRootProcessor;
import org.jboss.as.web.deployment.JBossWebParsingDeploymentProcessor;
import org.jboss.as.web.deployment.ServletContainerInitializerDeploymentProcessor;
import org.jboss.as.web.deployment.TldParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WarAnnotationDeploymentProcessor;
import org.jboss.as.web.deployment.WarClassloadingDependencyProcessor;
import org.jboss.as.web.deployment.WarDeploymentInitializingProcessor;
import org.jboss.as.web.deployment.WarDeploymentProcessor;
import org.jboss.as.web.deployment.WarMetaDataProcessor;
import org.jboss.as.web.deployment.WarStructureDeploymentProcessor;
import org.jboss.as.web.deployment.WebFragmentParsingDeploymentProcessor;
import org.jboss.as.web.deployment.WebInitializeInOrderProcessor;
import org.jboss.as.web.deployment.WebParsingDeploymentProcessor;
import org.jboss.as.web.deployment.component.WebComponentProcessor;
import org.jboss.as.web.deployment.jsf.JsfAnnotationProcessor;
import org.jboss.as.web.deployment.jsf.JsfManagedBeanProcessor;
import org.jboss.as.web.deployment.jsf.JsfVersionProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;

/**
 * Adds the web subsystem.
 *
 * @author Emanuel Muckenhuber
 */
class WebSubsystemAdd extends AbstractBoottimeAddStepHandler implements DescriptionProvider {

    static final WebSubsystemAdd INSTANCE = new WebSubsystemAdd();
    private static final String DEFAULT_VIRTUAL_SERVER = "default-host";
    private static final boolean DEFAULT_NATIVE = true;
    private static final String TEMP_DIR = "jboss.server.temp.dir";

    private WebSubsystemAdd() {
        //
    }

    @Override
    protected void populateModel(ModelNode operation, final Resource resource) {
        WebConfigurationHandlerUtils.initializeConfiguration(resource, operation);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) {
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model,
                                   ServiceVerificationHandler verificationHandler,
                                   List<ServiceController<?>> newControllers) throws OperationFailedException {
        final ModelNode config = operation.get(Constants.CONTAINER_CONFIG);
        final String defaultVirtualServer = operation.hasDefined(Constants.DEFAULT_VIRTUAL_SERVER) ?
                operation.get(Constants.DEFAULT_VIRTUAL_SERVER).asString() : DEFAULT_VIRTUAL_SERVER;
        final boolean useNative = operation.hasDefined(Constants.NATIVE) ?
                operation.get(Constants.NATIVE).asBoolean() : DEFAULT_NATIVE;
        final String instanceId = operation.hasDefined(Constants.INSTANCE_ID) ? operation.get(
                Constants.INSTANCE_ID).asString() : null;

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                final SharedWebMetaDataBuilder sharedWebBuilder = new SharedWebMetaDataBuilder(config.clone());
                final SharedTldsMetaDataBuilder sharedTldsBuilder = new SharedTldsMetaDataBuilder(config.clone());

                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_WAR_DEPLOYMENT_INIT, new WarDeploymentInitializingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_WAR, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder));
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT, new WebParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT_FRAGMENT, new WebFragmentParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_JSF_VERSION, new JsfVersionProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_JBOSS_WEB_DEPLOYMENT, new JBossWebParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_TLD_DEPLOYMENT, new TldParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_ANNOTATION_WAR, new WarAnnotationDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_COMPONENTS, new WebComponentProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_CONTEXT_ROOT, new EarContextRootProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_MERGE_METADATA, new WarMetaDataProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.POST_MODULE_JSF_MANAGED_BEANS, new JsfManagedBeanProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_WEB_INITIALIZE_IN_ORDER, new WebInitializeInOrderProcessor(defaultVirtualServer));

                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_WAR_MODULE, new WarClassloadingDependencyProcessor());

                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_JSF_MANAGED_BEANS, new JsfManagedBeanProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_SERVLET_INIT_DEPLOYMENT, new ServletContainerInitializerDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_JSF_ANNOTATIONS, new JsfAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_WAR_DEPLOYMENT, new WarDeploymentProcessor(defaultVirtualServer));
            }
        }, OperationContext.Stage.RUNTIME);

        final WebServerService service = new WebServerService(defaultVirtualServer, useNative, instanceId);
        newControllers.add(context.getServiceTarget().addService(WebSubsystemServices.JBOSS_WEB, service)
                .addDependency(AbstractPathService.pathNameOf(TEMP_DIR), String.class, service.getPathInjector())
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, service.getMbeanServer())
                .setInitialMode(Mode.ON_DEMAND)
                .install());

    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    @Override
    public ModelNode getModelDescription(Locale locale) {
        return WebSubsystemDescriptions.getSubsystemAddDescription(locale);
    }
}
