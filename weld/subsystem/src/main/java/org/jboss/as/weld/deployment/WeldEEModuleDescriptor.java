/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.weld.deployment;

import static org.jboss.as.ee.structure.Attachments.DEPLOYMENT_TYPE;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
import org.jboss.as.weld.logging.WeldLogger;
import org.jboss.weld.bootstrap.spi.EEModuleDescriptor;
import org.jboss.weld.bootstrap.spi.helpers.EEModuleDescriptorImpl;

/**
 * Implementation of Weld's {@link EEModuleDescriptor}
 *
 * @author Jozef Hartinger
 *
 */
public class WeldEEModuleDescriptor extends EEModuleDescriptorImpl implements EEModuleDescriptor {

    public static WeldEEModuleDescriptor of(String id, DeploymentUnit deploymentUnit) {
        final DeploymentType deploymentType = deploymentUnit.getAttachment(DEPLOYMENT_TYPE);
        if (deploymentType == null) {
            if (EjbDeploymentMarker.isEjbDeployment(deploymentUnit)) {
                return new WeldEEModuleDescriptor(id, ModuleType.EJB_JAR);
            }
            return null;
        }
        switch (deploymentType) {
            case WAR:
                return new WeldEEModuleDescriptor(id, ModuleType.WEB);
            case EAR:
                return new WeldEEModuleDescriptor(id, ModuleType.EAR);
            case EJB_JAR:
                return new WeldEEModuleDescriptor(id, ModuleType.EJB_JAR);
            case APPLICATION_CLIENT:
                return new WeldEEModuleDescriptor(id, ModuleType.APPLICATION_CLIENT);
            default:
                throw WeldLogger.DEPLOYMENT_LOGGER.unknownDeploymentType(deploymentType);
        }

    }

    private WeldEEModuleDescriptor(String id, ModuleType moduleType) {
        super(id, moduleType);
    }
}
