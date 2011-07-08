/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.ee.component;

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;

import java.util.Collection;

/**
 * Deployment processor responsible for creating a {@link EEModuleConfiguration} from a {@link EEModuleDescription} and
 * populating it with component and class configurations
 *
 * @author John Bailey
 */
public class EEModuleConfigurationProcessor implements DeploymentUnitProcessor {

    private static final Logger logger = Logger.getLogger(EEModuleConfigurationProcessor.class);

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final EEApplicationDescription applicationDescription = deploymentUnit.getAttachment(Attachments.EE_APPLICATION_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        if (moduleDescription == null) {
            return;
        }
        if (module == null) {
            return;
        }

        final EEModuleConfiguration moduleConfiguration = new EEModuleConfiguration(moduleDescription, phaseContext, module);
        deploymentUnit.putAttachment(Attachments.EE_MODULE_CONFIGURATION, moduleConfiguration);

        final Collection<ComponentDescription> componentDescriptions = moduleDescription.getComponentDescriptions();
        if (componentDescriptions != null) {
            for (ComponentDescription componentDescription : componentDescriptions) {
                logger.debug("Configuring component class: " + componentDescription.getComponentClassName() + " named " + componentDescription.getComponentName());
                final ComponentConfiguration componentConfiguration = componentDescription.createConfiguration(applicationDescription);
                for (ComponentConfigurator componentConfigurator : componentDescription.getConfigurators()) {
                    componentConfigurator.configure(phaseContext, componentDescription, componentConfiguration);
                }
                moduleConfiguration.addComponentConfiguration(componentConfiguration);
            }
        }

    }

    public void undeploy(DeploymentUnit context) {
    }
}
