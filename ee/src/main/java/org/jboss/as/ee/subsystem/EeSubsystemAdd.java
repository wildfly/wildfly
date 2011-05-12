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

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.ee.beanvalidation.BeanValidationFactoryDeployer;
import org.jboss.as.ee.component.AroundInvokeAnnotationParsingProcessor;
import org.jboss.as.ee.component.ComponentInstallProcessor;
import org.jboss.as.ee.component.EEModuleConfigurationProcessor;
import org.jboss.as.ee.component.InterceptorsAnnotationParsingProcessor;
import org.jboss.as.ee.component.EEModuleInitialProcessor;
import org.jboss.as.ee.component.EEModuleNameProcessor;
import org.jboss.as.ee.component.ResourceReferenceProcessor;
import org.jboss.as.ee.component.LifecycleAnnotationParsingProcessor;
import org.jboss.as.ee.component.ModuleJndiBindingProcessor;
import org.jboss.as.ee.component.ResourceInjectionAnnotationParsingProcessor;
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
import org.jboss.as.ee.structure.InitalizeInOrderProcessor;
import org.jboss.as.ee.structure.JBossAppMetaDataParsingProcessor;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Handler for adding the ee subsystem.
 *
 * @author Weston M. Price
 * @author Emanuel Muckenhuber
 */
public class EeSubsystemAdd implements ModelAddOperationHandler, BootOperationHandler {

    private static final Logger logger = Logger.getLogger("org.jboss.as.ee");

    static final EeSubsystemAdd INSTANCE = new EeSubsystemAdd();

    private EeSubsystemAdd() {
        //
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public OperationResult execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        if (context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;
            logger.info("Activating EE subsystem");
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT_INIT, new EarInitializationProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_APP_XML_PARSE, new EarMetaDataParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_JBOSS_APP_XML_PARSE, new JBossAppMetaDataParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR, new EarStructureProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EJB_JAR_IN_EAR, new EjbJarDeploymentProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_MANAGED_BEAN_JAR_IN_EAR, new ManagedBeanSubDeploymentMarkingProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EE_MODULE_INIT, new EEModuleInitialProcessor());

            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_MANAGED_BEAN_ANNOTATION, new ManagedBeanAnnotationProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EE_MODULE_NAME, new EEModuleNameProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_LIB_CLASS_PATH, new EarLibManifestClassPathProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_INTERCEPTORS_ANNOTATION, new InterceptorsAnnotationParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_LIEFCYCLE_ANNOTATION, new LifecycleAnnotationParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_AROUNDINVOKE_ANNOTATION, new AroundInvokeAnnotationParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_RESOURCE_INJECTION_ANNOTATION, new ResourceInjectionAnnotationParsingProcessor());

            updateContext.addDeploymentProcessor(Phase.DEPENDENCIES, Phase.DEPENDENCIES_MANAGED_BEAN, new JavaEEDependencyProcessor());

            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_VALIDATOR_FACTORY, new BeanValidationFactoryDeployer());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_MODULE_EAR_DEPENDENCY, new EarDependencyProcessor());
            updateContext.addDeploymentProcessor(Phase.POST_MODULE, Phase.POST_INITIALIZE_IN_ORDER, new InitalizeInOrderProcessor());

            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_ENV_ENTRY, new ResourceReferenceProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_CONTEXT, new ModuleContextProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_APP_CONTEXT, new ApplicationContextProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_JNDI_BINDINGS, new ModuleJndiBindingProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_MODULE_CONFIG, new EEModuleConfigurationProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EAR_AGGREGATE_COMPONENT_INDEX, new ComponentAggregationProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT, new ComponentInstallProcessor());
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        context.getSubModel().setEmptyObject();

        resultHandler.handleResultComplete();
        return new BasicOperationResult(compensatingOperation);
    }
}
