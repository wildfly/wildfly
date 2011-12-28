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

package org.jboss.as.ee.subsystem;

import java.util.List;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.beanvalidation.BeanValidationFactoryDeployer;
import org.jboss.as.ee.beanvalidation.BeanValidationFactoryResourceReferenceProcessor;
import org.jboss.as.ee.beanvalidation.BeanValidationResourceReferenceProcessor;
import org.jboss.as.ee.component.deployers.ApplicationClassesAggregationProcessor;
import org.jboss.as.ee.component.deployers.AroundInvokeAnnotationParsingProcessor;
import org.jboss.as.ee.component.deployers.ComponentInstallProcessor;
import org.jboss.as.ee.component.deployers.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.component.deployers.EEAnnotationProcessor;
import org.jboss.as.ee.component.deployers.EECleanUpProcessor;
import org.jboss.as.ee.component.deployers.EEDistinctNameProcessor;
import org.jboss.as.ee.component.deployers.EEModuleConfigurationProcessor;
import org.jboss.as.ee.component.deployers.EEModuleInitialProcessor;
import org.jboss.as.ee.component.deployers.EEModuleNameProcessor;
import org.jboss.as.ee.component.deployers.EEResourceReferenceProcessorRegistry;
import org.jboss.as.ee.component.deployers.EarApplicationNameProcessor;
import org.jboss.as.ee.component.deployers.EarMessageDestinationProcessor;
import org.jboss.as.ee.component.deployers.InterceptorAnnotationProcessor;
import org.jboss.as.ee.component.deployers.LifecycleAnnotationParsingProcessor;
import org.jboss.as.ee.component.deployers.MessageDestinationResolutionProcessor;
import org.jboss.as.ee.component.deployers.ModuleJndiBindingProcessor;
import org.jboss.as.ee.component.deployers.ResourceInjectionAnnotationParsingProcessor;
import org.jboss.as.ee.component.deployers.ResourceReferenceProcessor;
import org.jboss.as.ee.datasource.DataSourceDefinitionAnnotationParser;
import org.jboss.as.ee.datasource.DataSourceDefinitionDeploymentDescriptorParser;
import org.jboss.as.ee.managedbean.processors.JavaEEDependencyProcessor;
import org.jboss.as.ee.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.ee.managedbean.processors.ManagedBeanSubDeploymentMarkingProcessor;
import org.jboss.as.ee.naming.ApplicationContextProcessor;
import org.jboss.as.ee.naming.ModuleContextProcessor;
import org.jboss.as.ee.structure.ApplicationClientDeploymentProcessor;
import org.jboss.as.ee.structure.ComponentAggregationProcessor;
import org.jboss.as.ee.structure.EJBClientDescriptorParsingProcessor;
import org.jboss.as.ee.structure.EarDependencyProcessor;
import org.jboss.as.ee.structure.EarInitializationProcessor;
import org.jboss.as.ee.structure.EarMetaDataParsingProcessor;
import org.jboss.as.ee.structure.EarStructureProcessor;
import org.jboss.as.ee.structure.EjbJarDeploymentProcessor;
import org.jboss.as.ee.structure.GlobalModuleDependencyProcessor;
import org.jboss.as.ee.structure.InitializeInOrderProcessor;
import org.jboss.as.naming.management.JndiViewExtensionRegistry;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

import static org.jboss.as.ee.EeLogger.ROOT_LOGGER;

/**
 * Handler for adding the ee subsystem.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor;
    private final GlobalModuleDependencyProcessor moduleDependencyProcessor;


    public EeSubsystemAdd(final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor,
                          final GlobalModuleDependencyProcessor moduleDependencyProcessor) {
        this.isolationProcessor = isolationProcessor;
        this.moduleDependencyProcessor = moduleDependencyProcessor;
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        GlobalModulesDefinition.INSTANCE.validateAndSet(operation, model);
        EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.validateAndSet(operation, model);
    }

    protected void performBoottime(OperationContext context, final ModelNode operation, final ModelNode model,
                                   final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;
        final EEJndiViewExtension extension = new EEJndiViewExtension();
        context.getServiceTarget().addService(EEJndiViewExtension.SERVICE_NAME, extension)
                .addDependency(JndiViewExtensionRegistry.SERVICE_NAME, JndiViewExtensionRegistry.class, extension.getRegistryInjector())
                .addListener(verificationHandler)
                .install();


        EEResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(BeanValidationFactoryResourceReferenceProcessor.INSTANCE);
        EEResourceReferenceProcessorRegistry.registerResourceReferenceProcessor(BeanValidationResourceReferenceProcessor.INSTANCE);

        final ModelNode globalModules = GlobalModulesDefinition.INSTANCE.resolveModelAttribute(context, model);
        // see if the ear subdeployment isolation flag is set. By default, we don't isolate subdeployments, so that
        // they can see each other's classes.
        final boolean earSubDeploymentsIsolated = EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.resolveModelAttribute(context, model).asBoolean();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                moduleDependencyProcessor.setGlobalModules(globalModules);
                isolationProcessor.setEarSubDeploymentsIsolated(earSubDeploymentsIsolated);

                ROOT_LOGGER.debug("Activating EE subsystem");
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT_INIT, new EarInitializationProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_APP_XML_PARSE, new EarMetaDataParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_JBOSS_EJB_CLIENT_XML_PARSE, new EJBClientDescriptorParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EJB_EAR_APPLICATION_NAME, new EarApplicationNameProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR, new EarStructureProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EJB_JAR_IN_EAR, new EjbJarDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_APPLICATION_CLIENT_IN_EAR, new ApplicationClientDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MANAGED_BEAN_JAR_IN_EAR, new ManagedBeanSubDeploymentMarkingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EE_MODULE_INIT, new EEModuleInitialProcessor());

                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EE_MODULE_NAME, new EEModuleNameProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EE_ANNOTATIONS, new EEAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_LIEFCYCLE_ANNOTATION, new LifecycleAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_AROUNDINVOKE_ANNOTATION, new AroundInvokeAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_SUBDEPLOYMENTS_ISOLATION_DEFAULT, isolationProcessor);
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DATA_SOURCE_DEFINITION_ANNOTATION, new DataSourceDefinitionAnnotationParser());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DISTINCT_NAME, new EEDistinctNameProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_MESSAGE_DESTINATIONS, new EarMessageDestinationProcessor());


                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MANAGED_BEAN, new JavaEEDependencyProcessor());
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_GLOBAL_MODULES, moduleDependencyProcessor);
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_EE_CLASS_DESCRIPTIONS, new ApplicationClassesAggregationProcessor());

                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_VALIDATOR_FACTORY, new BeanValidationFactoryDeployer());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EAR_DEPENDENCY, new EarDependencyProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_INITIALIZE_IN_ORDER, new InitializeInOrderProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_INJECTION_ANNOTATION, new ResourceInjectionAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_ENV_ENTRY, new ResourceReferenceProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_DATASOURCE_REF, new DataSourceDefinitionDeploymentDescriptorParser());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_INTERCEPTOR_ANNOTATIONS, new InterceptorAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_NAMING_CONTEXT, new ModuleContextProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_APP_NAMING_CONTEXT, new ApplicationContextProcessor());

                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_RESOLVE_MESSAGE_DESTINATIONS, new MessageDestinationResolutionProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_COMPONENT_AGGREGATION, new ComponentAggregationProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_JNDI_BINDINGS, new ModuleJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG, new EEModuleConfigurationProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT, new ComponentInstallProcessor());

                processorTarget.addDeploymentProcessor(Phase.CLEANUP, Phase.CLEANUP_EE, new EECleanUpProcessor());

                if (!appclient) {
                    processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MANAGED_BEAN_ANNOTATION, new ManagedBeanAnnotationProcessor());
                }


            }
        }, OperationContext.Stage.RUNTIME);

    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
