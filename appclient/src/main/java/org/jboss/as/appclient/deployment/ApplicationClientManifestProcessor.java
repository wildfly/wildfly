/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
                    final Class<?> clazz;
                    try {
                        clazz = module.getClassLoader().loadClass(mainClass);
                    } catch (Throwable e) {
                        throw AppClientLogger.ROOT_LOGGER.cannotLoadAppClientMainClass(e);
                    }
                    deploymentUnit.putAttachment(AppClientAttachments.MAIN_CLASS, clazz);
                    final ApplicationClientComponentDescription description = new ApplicationClientComponentDescription(clazz.getName(), moduleDescription, deploymentUnit.getServiceName());
                    moduleDescription.addComponent(description);
                    deploymentUnit.putAttachment(AppClientAttachments.APPLICATION_CLIENT_COMPONENT, description);

                    final DeploymentDescriptorEnvironment environment = deploymentUnit.getAttachment(org.jboss.as.ee.component.Attachments.MODULE_DEPLOYMENT_DESCRIPTOR_ENVIRONMENT);
                    if (environment != null) {
                        DescriptorEnvironmentLifecycleMethodProcessor.handleMethods(environment, moduleDescription, mainClass);
                    }
                }
            }
        }
    }
}

