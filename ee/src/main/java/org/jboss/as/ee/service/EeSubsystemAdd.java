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

package org.jboss.as.ee.service;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;

import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.ee.component.processor.ComponentClassLoadingProcessor;
import org.jboss.as.ee.component.processor.ComponentInstallProcessor;
import org.jboss.as.ee.component.processor.InterceptorAnnotationParsingProcessor;
import org.jboss.as.ee.component.processor.InterceptorInstallProcessor;
import org.jboss.as.ee.component.processor.LifecycleAnnotationParsingProcessor;
import org.jboss.as.ee.component.processor.LifecycleInstallProcessor;
import org.jboss.as.ee.component.processor.ResourceInjectionAnnotationParsingProcessor;
import org.jboss.as.ee.component.processor.ResourceInjectionInstallProcessor;
import org.jboss.as.ee.naming.ApplicationContextProcessor;
import org.jboss.as.ee.naming.ModuleContextProcessor;
import org.jboss.as.ee.structure.EarInitializationProcessor;
import org.jboss.as.ee.structure.EarLibManifestClassPathProcessor;
import org.jboss.as.ee.structure.EarMetaDataParsingProcessor;
import org.jboss.as.ee.structure.EarStructureProcessor;
import org.jboss.as.ee.structure.EjbJarDeploymentProcessor;
import org.jboss.as.ee.structure.JBossAppMetaDataParsingProcessor;
import org.jboss.as.server.BootOperationContext;
import org.jboss.as.server.BootOperationHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

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

    /** {@inheritDoc} */
    @Override
    public Cancellable execute(OperationContext context, ModelNode operation, ResultHandler resultHandler) {
        if(context instanceof BootOperationContext) {
            final BootOperationContext updateContext = (BootOperationContext) context;
            logger.info("Activating EE subsystem");
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT_INIT, new EarInitializationProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_APP_XML_PARSE, new EarMetaDataParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_JBOSS_APP_XML_PARSE, new JBossAppMetaDataParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR, new EarStructureProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_EAR_LIB_CLASS_PATH, new EarLibManifestClassPathProcessor());

            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_BEAN_LIEFCYCLE_ANNOTATION, new LifecycleAnnotationParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_BEAN_INTERCEPTOR_ANNOTATION, new InterceptorAnnotationParsingProcessor());
            updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_BEAN_RESOURCE_INJECTION_ANNOTATION, new ResourceInjectionAnnotationParsingProcessor());

            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_CONTEXT, new ModuleContextProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_APP_CONTEXT, new ApplicationContextProcessor());

            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT_CLASSLOAD, new ComponentClassLoadingProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT_LIFECYCLES, new LifecycleInstallProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT_INTERCEPTORS, new InterceptorInstallProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT_INJECTIONS, new ResourceInjectionInstallProcessor());
            updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_EE_COMPONENT, new ComponentInstallProcessor());
        }

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        context.getSubModel().setEmptyObject();

        resultHandler.handleResultComplete(compensatingOperation);

        return Cancellable.NULL;
    }

}
