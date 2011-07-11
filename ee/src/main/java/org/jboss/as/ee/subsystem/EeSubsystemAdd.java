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
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ee.beanvalidation.BeanValidationFactoryDeployer;
import org.jboss.as.ee.component.AroundInvokeAnnotationParsingProcessor;
import org.jboss.as.ee.component.ComponentInstallProcessor;
import org.jboss.as.ee.component.DefaultEarSubDeploymentsIsolationProcessor;
import org.jboss.as.ee.component.EEClassConfigurationProcessor;
import org.jboss.as.ee.component.EEModuleConfigurationProcessor;
import org.jboss.as.ee.component.EEModuleInitialProcessor;
import org.jboss.as.ee.component.EEModuleNameProcessor;
import org.jboss.as.ee.component.InterceptorsAnnotationParsingProcessor;
import org.jboss.as.ee.component.LifecycleAnnotationParsingProcessor;
import org.jboss.as.ee.component.ModuleJndiBindingProcessor;
import org.jboss.as.ee.component.ResourceInjectionAnnotationParsingProcessor;
import org.jboss.as.ee.component.ResourceReferenceProcessor;
import org.jboss.as.ee.datasource.DataSourceDefinitionAnnotationParser;
import org.jboss.as.ee.datasource.DataSourceDefinitionDeploymentDescriptorParser;
import org.jboss.as.ee.managedbean.processors.JavaEEDependencyProcessor;
import org.jboss.as.ee.managedbean.processors.ManagedBeanAnnotationProcessor;
import org.jboss.as.ee.managedbean.processors.ManagedBeanSubDeploymentMarkingProcessor;
import org.jboss.as.ee.naming.ApplicationContextProcessor;
import org.jboss.as.ee.naming.ModuleContextProcessor;
import org.jboss.as.ee.structure.ComponentAggregationProcessor;
import org.jboss.as.ee.structure.EarDependencyProcessor;
import org.jboss.as.ee.structure.EarInitializationProcessor;
import org.jboss.as.ee.structure.EarLibManifestClassPathProcessor;
import org.jboss.as.ee.structure.EarMetaDataParsingProcessor;
import org.jboss.as.ee.structure.EarStructureProcessor;
import org.jboss.as.ee.structure.EjbJarDeploymentProcessor;
import org.jboss.as.ee.structure.GlobalModuleDependencyProcessor;
import org.jboss.as.ee.structure.InitalizeInOrderProcessor;
import org.jboss.as.ee.structure.JBossAppMetaDataParsingProcessor;
import org.jboss.as.naming.management.JndiViewExtensionRegistry;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceController;

/**
 * Handler for adding the ee subsystem.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeSubsystemAdd extends AbstractBoottimeAddStepHandler {

    private static final Logger logger = Logger.getLogger("org.jboss.as.ee");

    static final EeSubsystemAdd INSTANCE = new EeSubsystemAdd();

    private EeSubsystemAdd() {
        //
    }

    protected void populateModel(ModelNode operation, ModelNode model) {
        final ModelNode globalModules = operation.get(CommonAttributes.GLOBAL_MODULES);
        if (globalModules.isDefined()) {
            model.get(CommonAttributes.GLOBAL_MODULES).set(globalModules.clone());
        }
    }

    protected void performBoottime(OperationContext context, final ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) {
        final EEJndiViewExtension extension = new EEJndiViewExtension();
        context.getServiceTarget().addService(EEJndiViewExtension.SERVICE_NAME, extension)
                .addDependency(JndiViewExtensionRegistry.SERVICE_NAME, JndiViewExtensionRegistry.class, extension.getRegistryInjector())
                .addListener(verificationHandler)
                .install();

        context.addStep(new AbstractDeploymentChainStep() {
            protected void execute(DeploymentProcessorTarget processorTarget) {
                final ModelNode globalModules = operation.get(CommonAttributes.GLOBAL_MODULES);
                // see if the ear subdeployment isolation flag is set. By default, we don't isolate subdeployments, so that
                // they can see each other's classes.
                final Boolean earSubDeploymentsIsolated = operation.hasDefined(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName())
                    ? operation.get(Element.EAR_SUBDEPLOYMENTS_ISOLATED.getLocalName()).asBoolean()
                    : Boolean.FALSE;

                logger.info("Activating EE subsystem");
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT_INIT, new EarInitializationProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_APP_XML_PARSE, new EarMetaDataParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_JBOSS_APP_XML_PARSE, new JBossAppMetaDataParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR, new EarStructureProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EJB_JAR_IN_EAR, new EjbJarDeploymentProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MANAGED_BEAN_JAR_IN_EAR, new ManagedBeanSubDeploymentMarkingProcessor());
                processorTarget.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EE_MODULE_INIT, new EEModuleInitialProcessor());

                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MANAGED_BEAN_ANNOTATION, new ManagedBeanAnnotationProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EE_MODULE_NAME, new EEModuleNameProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_LIB_CLASS_PATH, new EarLibManifestClassPathProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_INTERCEPTORS_ANNOTATION, new InterceptorsAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_LIEFCYCLE_ANNOTATION, new LifecycleAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_AROUNDINVOKE_ANNOTATION, new AroundInvokeAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_SUBDEPLOYMENTS_ISOLATION_DEFAULT, new DefaultEarSubDeploymentsIsolationProcessor(earSubDeploymentsIsolated));
                processorTarget.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_DATA_SOURCE_DEFINITION_ANNOTATION, new DataSourceDefinitionAnnotationParser());

                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MANAGED_BEAN, new JavaEEDependencyProcessor());
                processorTarget.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_GLOBAL_MODULES, new GlobalModuleDependencyProcessor(globalModules));

                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_VALIDATOR_FACTORY, new BeanValidationFactoryDeployer());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EAR_DEPENDENCY, new EarDependencyProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_INITIALIZE_IN_ORDER, new InitalizeInOrderProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_INJECTION_ANNOTATION, new ResourceInjectionAnnotationParsingProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_ENV_ENTRY, new ResourceReferenceProcessor());
                processorTarget.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_DATASOURCE_REF, new DataSourceDefinitionDeploymentDescriptorParser());

                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_COMPONENT_AGGREGATION, new ComponentAggregationProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_CONTEXT, new ModuleContextProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_APP_CONTEXT, new ApplicationContextProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_JNDI_BINDINGS, new ModuleJndiBindingProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_CLASS_CONFIG, new EEClassConfigurationProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG, new EEModuleConfigurationProcessor());
                processorTarget.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT, new ComponentInstallProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    protected boolean requiresRuntimeVerification() {
        return false;
    }
}
