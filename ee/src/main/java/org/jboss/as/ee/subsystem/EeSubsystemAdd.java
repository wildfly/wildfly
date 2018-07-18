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

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ee.component.ReflectiveClassIntrospector;
import org.jboss.as.ee.component.deployers.ApplicationClassesAggregationProcessor;
import org.jboss.as.ee.component.deployers.AroundInvokeAnnotationParsingProcessor;
import org.jboss.as.ee.component.deployers.ComponentInstallProcessor;
import org.jboss.as.ee.component.deployers.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.component.deployers.DescriptorEnvironmentLifecycleMethodProcessor;
import org.jboss.as.ee.component.deployers.EEAnnotationProcessor;
import org.jboss.as.ee.component.deployers.EECleanUpProcessor;
import org.jboss.as.ee.component.deployers.EEDefaultPermissionsProcessor;
import org.jboss.as.ee.component.deployers.EEDistinctNameProcessor;
import org.jboss.as.ee.component.deployers.EEModuleConfigurationProcessor;
import org.jboss.as.ee.component.deployers.EEModuleInitialProcessor;
import org.jboss.as.ee.component.deployers.EEModuleNameProcessor;
import org.jboss.as.ee.component.deployers.EarApplicationNameProcessor;
import org.jboss.as.ee.component.deployers.EarMessageDestinationProcessor;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentDefaultBindingProcessor;
import org.jboss.as.ee.concurrent.deployers.EEConcurrentContextProcessor;
import org.jboss.as.ee.naming.InApplicationClientBindingProcessor;
import org.jboss.as.ee.component.deployers.InterceptorAnnotationProcessor;
import org.jboss.as.ee.component.deployers.LifecycleAnnotationParsingProcessor;
import org.jboss.as.ee.component.deployers.MessageDestinationResolutionProcessor;
import org.jboss.as.ee.component.deployers.ModuleJndiBindingProcessor;
import org.jboss.as.ee.component.deployers.ResourceInjectionAnnotationParsingProcessor;
import org.jboss.as.ee.component.deployers.ResourceReferenceProcessor;
import org.jboss.as.ee.component.deployers.ResourceReferenceRegistrySetupProcessor;
import org.jboss.as.ee.naming.InstanceNameBindingProcessor;
import org.jboss.as.ee.structure.AppJBossAllParser;
import org.jboss.as.ee.structure.DescriptorPropertyReplacementProcessor;
import org.jboss.as.ee.managedbean.processors.JavaEEDependencyProcessor;
import org.jboss.as.ee.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.ee.managedbean.processors.ManagedBeanSubDeploymentMarkingProcessor;
import org.jboss.as.ee.metadata.property.DeploymentPropertiesProcessor;
import org.jboss.as.ee.metadata.property.DeploymentPropertyResolverProcessor;
import org.jboss.as.ee.metadata.property.PropertyResolverProcessor;
import org.jboss.as.ee.metadata.property.SystemPropertyResolverProcessor;
import org.jboss.as.ee.metadata.property.VaultPropertyResolverProcessor;
import org.jboss.as.ee.naming.ApplicationContextProcessor;
import org.jboss.as.ee.naming.ModuleContextProcessor;
import org.jboss.as.ee.structure.AnnotationPropertyReplacementProcessor;
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
import org.jboss.as.server.deployment.jbossallxml.JBossAllXmlParserRegisteringProcessor;
import org.jboss.dmr.ModelNode;
import org.jboss.metadata.ear.jboss.JBossAppMetaData;

import static org.jboss.as.ee.logging.EeLogger.ROOT_LOGGER;

/**
 * Handler for adding the ee subsystem.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor;
    private final GlobalModuleDependencyProcessor moduleDependencyProcessor;
    private final DescriptorPropertyReplacementProcessor specDescriptorPropertyReplacementProcessor;
    private final DescriptorPropertyReplacementProcessor jbossDescriptorPropertyReplacementProcessor;
    private final AnnotationPropertyReplacementProcessor ejbAnnotationPropertyReplacementProcessor;


    public EeSubsystemAdd(final DefaultEarSubDeploymentsIsolationProcessor isolationProcessor,
                          final GlobalModuleDependencyProcessor moduleDependencyProcessor,
                          final DescriptorPropertyReplacementProcessor specDescriptorPropertyReplacementProcessor,
                          final DescriptorPropertyReplacementProcessor jbossDescriptorPropertyReplacementProcessor,
                          final AnnotationPropertyReplacementProcessor ejbAnnotationPropertyReplacementProcessor) {
        this.isolationProcessor = isolationProcessor;
        this.moduleDependencyProcessor = moduleDependencyProcessor;
        this.specDescriptorPropertyReplacementProcessor = specDescriptorPropertyReplacementProcessor;
        this.jbossDescriptorPropertyReplacementProcessor = jbossDescriptorPropertyReplacementProcessor;
        this.ejbAnnotationPropertyReplacementProcessor = ejbAnnotationPropertyReplacementProcessor;
    }

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {

        for (AttributeDefinition ad : EeSubsystemRootResource.ATTRIBUTES) {
            ad.validateAndSet(operation, model);
        }
    }

    protected void performBoottime(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();
        final EEJndiViewExtension extension = new EEJndiViewExtension();
        context.getServiceTarget().addService(EEJndiViewExtension.SERVICE_NAME, extension)
                .addDependency(JndiViewExtensionRegistry.SERVICE_NAME, JndiViewExtensionRegistry.class, extension.getRegistryInjector())
                .install();

        final boolean appclient = context.getProcessType() == ProcessType.APPLICATION_CLIENT;

        final ModelNode globalModules = GlobalModulesDefinition.INSTANCE.resolveModelAttribute(context, model);
        // see if the ear subdeployment isolation flag is set. By default, we don't isolate subdeployments, so that
        // they can see each other's classes.
        final boolean earSubDeploymentsIsolated = EeSubsystemRootResource.EAR_SUBDEPLOYMENTS_ISOLATED.resolveModelAttribute(context, model).asBoolean();
        final boolean specDescriptorPropertyReplacement = EeSubsystemRootResource.SPEC_DESCRIPTOR_PROPERTY_REPLACEMENT.resolveModelAttribute(context, model).asBoolean();
        final boolean jbossDescriptorPropertyReplacement = EeSubsystemRootResource.JBOSS_DESCRIPTOR_PROPERTY_REPLACEMENT.resolveModelAttribute(context, model).asBoolean();
        final boolean ejbAnnotationPropertyReplacement = EeSubsystemRootResource.ANNOTATION_PROPERTY_REPLACEMENT.resolveModelAttribute(context, model).asBoolean();

        moduleDependencyProcessor.setGlobalModules(GlobalModulesDefinition.createModuleList(context, globalModules));
        isolationProcessor.setEarSubDeploymentsIsolated(earSubDeploymentsIsolated);
        specDescriptorPropertyReplacementProcessor.setDescriptorPropertyReplacement(specDescriptorPropertyReplacement);
        jbossDescriptorPropertyReplacementProcessor.setDescriptorPropertyReplacement(jbossDescriptorPropertyReplacement);
        ejbAnnotationPropertyReplacementProcessor.setDescriptorPropertyReplacement(ejbAnnotationPropertyReplacement);

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {

                ROOT_LOGGER.debug("Activating EE subsystem");

                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_DEPLOYMENT_PROPERTIES, new DeploymentPropertiesProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_DEPLOYMENT_PROPERTY_RESOLVER, new DeploymentPropertyResolverProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_VAULT_PROPERTY_RESOLVER, new VaultPropertyResolverProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_SYSTEM_PROPERTY_RESOLVER, new SystemPropertyResolverProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_PROPERTY_RESOLVER, new PropertyResolverProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_REGISTER_JBOSS_ALL_EE_APP, new JBossAllXmlParserRegisteringProcessor<JBossAppMetaData>(AppJBossAllParser.ROOT_ELEMENT, AppJBossAllParser.ATTACHMENT_KEY, new AppJBossAllParser()));
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_SPEC_DESC_PROPERTY_REPLACEMENT, specDescriptorPropertyReplacementProcessor);
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_JBOSS_DESC_PROPERTY_REPLACEMENT, jbossDescriptorPropertyReplacementProcessor);
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_EJB_ANNOTATION_PROPERTY_REPLACEMENT, ejbAnnotationPropertyReplacementProcessor);
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT_INIT, new EarInitializationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EAR_APP_XML_PARSE, new EarMetaDataParsingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_JBOSS_EJB_CLIENT_XML_PARSE, new EJBClientDescriptorParsingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EJB_EAR_APPLICATION_NAME, new EarApplicationNameProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EAR, new EarStructureProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EJB_JAR_IN_EAR, new EjbJarDeploymentProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_APPLICATION_CLIENT_IN_EAR, new ApplicationClientDeploymentProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_MANAGED_BEAN_JAR_IN_EAR, new ManagedBeanSubDeploymentMarkingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EAR_SUB_DEPLYOMENTS_ISOLATED, isolationProcessor);
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_MODULE_INIT, new EEModuleInitialProcessor(context.getProcessType() == ProcessType.APPLICATION_CLIENT));
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_EE_RESOURCE_INJECTION_REGISTRY, new ResourceReferenceRegistrySetupProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.STRUCTURE, Phase.STRUCTURE_GLOBAL_MODULES, moduleDependencyProcessor);


                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_MODULE_NAME, new EEModuleNameProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EE_ANNOTATIONS, new EEAnnotationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_LIFECYCLE_ANNOTATION, new LifecycleAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_AROUNDINVOKE_ANNOTATION, new AroundInvokeAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DISTINCT_NAME, new EEDistinctNameProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_EAR_MESSAGE_DESTINATIONS, new EarMessageDestinationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_MANAGED_BEAN_ANNOTATION, new ManagedBeanAnnotationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.PARSE, Phase.PARSE_DESCRIPTOR_LIFECYCLE_METHOD_RESOLUTION, new DescriptorEnvironmentLifecycleMethodProcessor());

                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_EE_PERMISSIONS, new EEDefaultPermissionsProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_MANAGED_BEAN, new JavaEEDependencyProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.DEPENDENCIES, Phase.DEPENDENCIES_EE_CLASS_DESCRIPTIONS, new ApplicationClassesAggregationProcessor());

                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EAR_DEPENDENCY, new EarDependencyProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_INITIALIZE_IN_ORDER, new InitializeInOrderProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_INJECTION_ANNOTATION, new ResourceInjectionAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_ENV_ENTRY, new ResourceReferenceProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_INTERCEPTOR_ANNOTATIONS, new InterceptorAnnotationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_NAMING_CONTEXT, new ModuleContextProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_IN_APP_CLIENT, new InApplicationClientBindingProcessor(appclient));
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EE_INSTANCE_NAME, new InstanceNameBindingProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_APP_NAMING_CONTEXT, new ApplicationContextProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.POST_MODULE, Phase.POST_MODULE_EE_CONCURRENT_CONTEXT, new EEConcurrentContextProcessor());

                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_RESOLVE_MESSAGE_DESTINATIONS, new MessageDestinationResolutionProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_COMPONENT_AGGREGATION, new ComponentAggregationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_DEFAULT_BINDINGS_EE_CONCURRENCY, new EEConcurrentDefaultBindingProcessor());

                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_MODULE_JNDI_BINDINGS, new ModuleJndiBindingProcessor(appclient));
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG, new EEModuleConfigurationProcessor());
                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.INSTALL, Phase.INSTALL_EE_COMPONENT, new ComponentInstallProcessor());

                processorTarget.addDeploymentProcessor(EeExtension.SUBSYSTEM_NAME, Phase.CLEANUP, Phase.CLEANUP_EE, new EECleanUpProcessor());

            }
        }, OperationContext.Stage.RUNTIME);

        context.getServiceTarget().addService(ReflectiveClassIntrospector.SERVICE_NAME, new ReflectiveClassIntrospector()).install();
    }
}
