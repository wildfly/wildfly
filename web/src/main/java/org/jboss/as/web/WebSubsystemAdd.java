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

import javax.management.MBeanServer;

import org.jboss.as.clustering.web.DistributedCacheManagerFactory;
import org.jboss.as.clustering.web.DistributedCacheManagerFactoryService;
import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.services.path.PathManager;
import org.jboss.as.controller.services.path.PathManagerService;
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
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.value.InjectedValue;

/**
 * Adds the web subsystem.
 *
 * @author Emanuel Muckenhuber
 * @author Tomaz Cerar
 */
class WebSubsystemAdd extends AbstractBoottimeAddStepHandler {

    static final WebSubsystemAdd INSTANCE = new WebSubsystemAdd();
    private static final String TEMP_DIR = "jboss.server.temp.dir";

    private WebSubsystemAdd() {
        //
    }


    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        WebDefinition.DEFAULT_VIRTUAL_SERVER.validateAndSet(operation, model);
        WebDefinition.NATIVE.validateAndSet(operation, model);
        WebDefinition.INSTANCE_ID.validateAndSet(operation, model);
    }

    @Override
    protected void performBoottime(OperationContext context, ModelNode baseOperation, ModelNode model,
                                   ServiceVerificationHandler verificationHandler,
                                   List<ServiceController<?>> newControllers) throws OperationFailedException {
        ModelNode fullModel = Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS));
        final ModelNode config = resolveConfiguration(context, fullModel.get(Constants.CONFIGURATION));
        final String defaultVirtualServer = WebDefinition.DEFAULT_VIRTUAL_SERVER.resolveModelAttribute(context, fullModel).asString();

        final boolean useNative = WebDefinition.NATIVE.resolveModelAttribute(context, fullModel).asBoolean();
        final ModelNode instanceIdModel = WebDefinition.INSTANCE_ID.resolveModelAttribute(context, fullModel);
        final String instanceId = instanceIdModel.isDefined() ? instanceIdModel.asString() : null;

        context.addStep(new AbstractDeploymentChainStep() {
            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {

                final SharedWebMetaDataBuilder sharedWebBuilder = new SharedWebMetaDataBuilder(config.clone());
                final SharedTldsMetaDataBuilder sharedTldsBuilder = new SharedTldsMetaDataBuilder(config.clone());

                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_WAR_DEPLOYMENT_INIT, new WarDeploymentInitializingProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_WAR, new WarStructureDeploymentProcessor(sharedWebBuilder.create(), sharedTldsBuilder));
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT, new WebParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_DEPLOYMENT_FRAGMENT, new WebFragmentParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JSF_VERSION, new JsfVersionProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_JBOSS_WEB_DEPLOYMENT, new JBossWebParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_TLD_DEPLOYMENT, new TldParsingDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_ANNOTATION_WAR, new WarAnnotationDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_COMPONENTS, new WebComponentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EAR_CONTEXT_ROOT, new EarContextRootProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_MERGE_METADATA, new WarMetaDataProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.POST_MODULE_JSF_MANAGED_BEANS, new JsfManagedBeanProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_WEB_INITIALIZE_IN_ORDER, new WebInitializeInOrderProcessor(defaultVirtualServer));

                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_WAR_MODULE, new WarClassloadingDependencyProcessor());

                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_JSF_MANAGED_BEANS, new JsfManagedBeanProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_SERVLET_INIT_DEPLOYMENT, new ServletContainerInitializerDeploymentProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_JSF_ANNOTATIONS, new JsfAnnotationProcessor());
                processorTarget.addDeploymentProcessor(WebExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_WAR_DEPLOYMENT, new WarDeploymentProcessor(defaultVirtualServer));
            }
        }, OperationContext.Stage.RUNTIME);

        final ServiceTarget target = context.getServiceTarget();
        final WebServerService service = new WebServerService(defaultVirtualServer, useNative, instanceId, TEMP_DIR);
        newControllers.add(target.addService(WebSubsystemServices.JBOSS_WEB, service)
                .addDependency(PathManagerService.SERVICE_NAME, PathManager.class, service.getPathManagerInjector())
                .addDependency(DependencyType.OPTIONAL, ServiceName.JBOSS.append("mbean", "server"), MBeanServer.class, service.getMbeanServer())
                .setInitialMode(Mode.ON_DEMAND)
                .install());

        final DistributedCacheManagerFactory factory = new DistributedCacheManagerFactoryService().getValue();
        if (factory != null) {
            final InjectedValue<WebServer> server = new InjectedValue<WebServer>();
            newControllers.add(target.addService(DistributedCacheManagerFactoryService.JVM_ROUTE_REGISTRY_ENTRY_PROVIDER_SERVICE_NAME, new JvmRouteRegistryEntryProviderService(server))
                    .addDependency(WebSubsystemServices.JBOSS_WEB, WebServer.class, server)
                    .setInitialMode(Mode.ON_DEMAND)
                    .install());
            newControllers.addAll(factory.installServices(target));
        }
    }

    @Override
    protected boolean requiresRuntimeVerification() {
        return false;
    }

    private ModelNode resolveConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        ModelNode res = new ModelNode();
        ModelNode unresolvedContainer = model.get(Constants.CONTAINER);
        for (AttributeDefinition attr : WebContainerDefinition.CONTAINER_ATTRIBUTES) {
            res.get(Constants.CONTAINER).get(attr.getName()).set(attr.resolveModelAttribute(context, unresolvedContainer));
        }
        ModelNode unresolvedStaticResources = model.get(Constants.STATIC_RESOURCES);
        for (SimpleAttributeDefinition attr : WebStaticResources.STATIC_ATTRIBUTES) {
            res.get(Constants.STATIC_RESOURCES).get(attr.getName()).set(attr.resolveModelAttribute(context, unresolvedStaticResources));
        }
        ModelNode unresolvedJspConf = model.get(Constants.JSP_CONFIGURATION);
        for (SimpleAttributeDefinition attr : WebJSPDefinition.JSP_ATTRIBUTES) {
            res.get(Constants.JSP_CONFIGURATION).get(attr.getName()).set(attr.resolveModelAttribute(context, unresolvedJspConf));
        }

        return res;
    }
}
