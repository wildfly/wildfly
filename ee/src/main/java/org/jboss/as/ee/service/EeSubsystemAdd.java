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

import org.jboss.as.ee.container.processor.BeanContainerInstallProcessor;
import org.jboss.as.ee.container.processor.InterceptorAnnotationParsingProcessor;
import org.jboss.as.ee.container.processor.LifecycleAnnotationParsingProcessor;
import org.jboss.as.ee.container.processor.ResourceInjectionAnnotationParsingProcessor;
import org.jboss.as.ee.naming.ApplicationContextProcessor;
import org.jboss.as.ee.naming.ModuleContextProcessor;
import org.jboss.as.ee.structure.EarMetaDataParsingProcessor;
import org.jboss.as.ee.structure.EarInitializationProcessor;
import org.jboss.as.ee.structure.EarStructureProcessor;
import org.jboss.as.ee.structure.JBossAppMetaDataParsingProcessor;
import org.jboss.as.model.AbstractSubsystemAdd;
import org.jboss.as.model.BootUpdateContext;
import org.jboss.as.model.UpdateContext;
import org.jboss.as.model.UpdateResultHandler;
import org.jboss.as.server.deployment.Phase;
import org.jboss.logging.Logger;

/**
 * @author Weston M. Price
 *
 */
public final class EeSubsystemAdd extends AbstractSubsystemAdd<EeSubsystemElement> {

    private static final long serialVersionUID = -3501832241733737257L;

    private static final Logger logger = Logger.getLogger("org.jboss.as.ee");

    protected EeSubsystemAdd() {
        super(EeExtension.NAMESPACE);
    }

    @Override
    protected <P> void applyUpdate(UpdateContext updateContext, UpdateResultHandler<? super Void, P> resultHandler, P param) {
    }

    @Override
    protected void applyUpdateBootAction(BootUpdateContext updateContext) {
        logger.info("Activating EE subsystem");
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_DEPLOYMENT_INIT, new EarInitializationProcessor());
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_APP_XML_PARSE, new EarMetaDataParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR_JBOSS_APP_XML_PARSE, new JBossAppMetaDataParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.STRUCTURE, Phase.STRUCTURE_EAR, new EarStructureProcessor());

        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_BEAN_LIEFCYCLE_ANNOTATION, new LifecycleAnnotationParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_BEAN_INTERCEPTOR_ANNOTATION, new InterceptorAnnotationParsingProcessor());
        updateContext.addDeploymentProcessor(Phase.PARSE, Phase.PARSE_BEAN_RESOURCE_INJECTION_ANNOTATION, new ResourceInjectionAnnotationParsingProcessor());

        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_MODULE_CONTEXT, new ModuleContextProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_APP_CONTEXT, new ApplicationContextProcessor());
        updateContext.addDeploymentProcessor(Phase.INSTALL, Phase.INSTALL_BEAN_CONTAINER, new BeanContainerInstallProcessor());
    }

    @Override
    protected EeSubsystemElement createSubsystemElement() {
        return new EeSubsystemElement();
    }

}
