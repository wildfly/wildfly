/*
 * JBoss, Home of Professional Open Source
 * Copyright 2015, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.weld.deployment;

import static org.jboss.as.ee.structure.Attachments.DEPLOYMENT_TYPE;

import org.jboss.as.ee.structure.DeploymentType;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.EjbDeploymentMarker;
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
                throw new IllegalArgumentException("Unknown deployment type " + deploymentType);
        }

    }

    private WeldEEModuleDescriptor(String id, ModuleType moduleType) {
        super(id, moduleType);
    }
}
