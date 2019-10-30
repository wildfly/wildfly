/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.appclient.deployment;

import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.jboss.as.appclient.component.ApplicationClientComponentDescription;
import org.jboss.as.appclient.logging.AppClientLogger;
import org.jboss.as.ee.component.DeploymentDescriptorEnvironment;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.component.deployers.DescriptorEnvironmentLifecycleMethodProcessor;
import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.ee.structure.DeploymentTypeMarker;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;

/**
 * DUP that processes the manifest to get the main class
 *
 * @author Stuart Douglas
 */
public class ApplicationClientManifestProcessor implements DeploymentUnitProcessor {
    @Override
    public void deploy(final DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        if (!DeploymentTypeMarker.isType(DeploymentType.APPLICATION_CLIENT, deploymentUnit)) {
            return;
        }

        final ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        final Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        final EEModuleDescription moduleDescription = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.EE_MODULE_DESCRIPTION);

        final Manifest manifest = root.getAttachment(Attachments.MANIFEST);
        if (manifest != null) {
            Attributes main = manifest.getMainAttributes();
            if (main != null) {
                String mainClass = main.getValue("Main-Class");
                if (mainClass != null && !mainClass.isEmpty()) {
                    try {
                        final Class<?> clazz = module.getClassLoader().loadClass(mainClass);
                        deploymentUnit.putAttachment(AppClientAttachments.MAIN_CLASS, clazz);
                        final ApplicationClientComponentDescription description = new ApplicationClientComponentDescription(clazz.getName(), moduleDescription, deploymentUnit.getServiceName());
                        moduleDescription.addComponent(description);
                        deploymentUnit.putAttachment(AppClientAttachments.APPLICATION_CLIENT_COMPONENT, description);

                        final DeploymentDescriptorEnvironment environment = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);
                        if (environment != null) {
                            DescriptorEnvironmentLifecycleMethodProcessor.handleMethods(environment, moduleDescription, mainClass);
                        }
                    } catch (ClassNotFoundException e) {
                        throw AppClientLogger.ROOT_LOGGER.cannotLoadAppClientMainClass(e);
                    }

                }
            }
        }
    }

    @Override
    public void undeploy(final DeploymentUnit context) {

    }
}
