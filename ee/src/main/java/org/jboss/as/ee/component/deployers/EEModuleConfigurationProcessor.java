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

package org.jboss.as.ee.component.deployers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.ComponentConfiguration;
import org.jboss.as.ee.component.ComponentConfigurator;
import org.jboss.as.ee.component.ComponentDescription;
import org.jboss.as.ee.component.EEModuleConfiguration;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.reflect.DeploymentClassIndex;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.ee.EeLogger.ROOT_LOGGER;
import static org.jboss.as.ee.EeMessages.MESSAGES;

/**
 * Deployment processor responsible for creating a {@link org.jboss.as.ee.component.EEModuleConfiguration} from a {@link org.jboss.as.ee.component.EEModuleDescription} and
 * populating it with component and class configurations
 *
 * @author John Bailey
 */
public class EEModuleConfigurationProcessor implements DeploymentUnitProcessor {

    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final DeploymentClassIndex classIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.CLASS_INDEX);
        if (moduleDescription == null) {
            return;
        }
        if (module == null) {
            return;
        }

        final Set<ServiceName> failed = new HashSet<ServiceName>();

        final EEModuleConfiguration moduleConfiguration = new EEModuleConfiguration(moduleDescription);
        deploymentUnit.putAttachment(Attachments.EE_MODULE_CONFIGURATION, moduleConfiguration);

        final Iterator<ComponentDescription> iterator = moduleDescription.getComponentDescriptions().iterator();
            while (iterator.hasNext()) {
                final ComponentDescription componentDescription = iterator.next();
                ROOT_LOGGER.debugf("Configuring component class: %s named %s", componentDescription.getComponentClassName(),
                        componentDescription.getComponentName());
                final ComponentConfiguration componentConfiguration;
                try {
                    componentConfiguration = componentDescription.createConfiguration(classIndex.classIndex(componentDescription.getComponentClassName()), module.getClassLoader(), module.getModuleLoader());
                    for (final ComponentConfigurator componentConfigurator : componentDescription.getConfigurators()) {
                        componentConfigurator.configure(phaseContext, componentDescription, componentConfiguration);
                    }
                    moduleConfiguration.addComponentConfiguration(componentConfiguration);
                } catch (Exception e) {
                    if (componentDescription.isOptional()) {
                        ROOT_LOGGER.componentInstallationFailure(e, componentDescription.getComponentName());
                        failed.add(componentDescription.getStartServiceName());
                        failed.add(componentDescription.getCreateServiceName());
                        failed.add(componentDescription.getServiceName());
                        iterator.remove();
                    } else {
                        throw MESSAGES.cannotConfigureComponent(e, componentDescription.getComponentName());
                    }
                }
            }

        deploymentUnit.putAttachment(Attachments.FAILED_COMPONENTS, Collections.synchronizedSet(failed));

    }

    public void undeploy(DeploymentUnit context) {
    }
}
