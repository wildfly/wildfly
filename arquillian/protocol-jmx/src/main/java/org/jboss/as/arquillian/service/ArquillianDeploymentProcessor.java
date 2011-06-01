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

package org.jboss.as.arquillian.service;

import org.jboss.as.osgi.deployment.BundleInstallService;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.osgi.deployment.deployer.Deployment;

/**
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 */
public class ArquillianDeploymentProcessor {

    private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("arquillian", "deployment", "tracker");

    private final DeploymentUnit deploymentUnit;

    ArquillianDeploymentProcessor(DeploymentUnit deploymentUnit) {
        this.deploymentUnit = deploymentUnit;
    }

    void deploy(ServiceTarget serviceTarget) {

        ArquillianConfig arqConfig = deploymentUnit.getAttachment(ArquillianConfig.KEY);
        if (arqConfig == null)
            return;

        DeploymentTrackerService tracker = new DeploymentTrackerService(arqConfig);
        ServiceName serviceName = DeploymentTrackerService.getServiceName(deploymentUnit);
        ServiceBuilder<ArquillianConfig> serviceBuilder = serviceTarget.addService(serviceName, tracker);
        serviceBuilder.addDependency(ArquillianService.SERVICE_NAME);

        // If this is an OSGi deployment, add a dependency on the associated service
        Deployment osgiDeployment = OSGiDeploymentAttachment.getDeployment(deploymentUnit);
        if (osgiDeployment != null) {
            ServiceName dependencyName = BundleInstallService.getServiceName(deploymentUnit.getName());
            serviceBuilder.addDependency(dependencyName);
            osgiDeployment.setAutoStart(false);
        }
        serviceBuilder.install();
    }

    void undeploy() {
        ServiceName serviceName = DeploymentTrackerService.getServiceName(deploymentUnit);
        ServiceController<?> controller = deploymentUnit.getServiceRegistry().getService(serviceName);
        if(controller != null) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    private static class DeploymentTrackerService extends AbstractService<ArquillianConfig>{
        private final ArquillianConfig arqConfig;

        DeploymentTrackerService(ArquillianConfig arqConfig) {
            this.arqConfig = arqConfig;
        }

        static ServiceName getServiceName(DeploymentUnit deploymentUnit) {
            return SERVICE_NAME_BASE.append(deploymentUnit.getName());
        }

        @Override
        public ArquillianConfig getValue() throws IllegalStateException {
            return arqConfig;
        }
    }
}
