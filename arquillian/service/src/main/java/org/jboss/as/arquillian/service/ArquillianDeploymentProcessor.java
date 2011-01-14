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

import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.as.osgi.deployment.OSGiDeploymentAttachment;
import org.jboss.as.osgi.deployment.OSGiDeploymentService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.osgi.deployment.deployer.Deployment;

/**
 * [TODO]
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kabir Khan
 */
public class ArquillianDeploymentProcessor implements DeploymentUnitProcessor {

    private static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("arquillian", "deployment", "tracker");

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {

        ArquillianConfig arqConfig = phaseContext.getDeploymentUnit().getAttachment(ArquillianConfig.KEY);
        if (arqConfig == null)
            return;

        ServiceTarget serviceTarget = phaseContext.getServiceTarget();
        DeploymentTrackerService tracker = new DeploymentTrackerService(arqConfig);
        ServiceBuilder<Object> serviceBuilder = serviceTarget.addService(SERVICE_NAME_BASE.append(phaseContext.getDeploymentUnit().getName()), tracker);
        serviceBuilder.addDependency(ArquillianService.SERVICE_NAME, ArquillianService.class, tracker.injectedArquillianService);

        // If this is an OSGi deployment, add a dependency on the associated service
        Deployment osgiDeployment = OSGiDeploymentAttachment.getDeployment(phaseContext.getDeploymentUnit());
        if (osgiDeployment != null) {
            ServiceName serviceName = OSGiDeploymentService.getServiceName(phaseContext.getDeploymentUnit().getName());
            serviceBuilder.addDependency(serviceName);
            osgiDeployment.setAutoStart(false);
        }
        serviceBuilder.install();
    }

    public void undeploy(final DeploymentUnit context) {
        final ServiceName serviceName = SERVICE_NAME_BASE.append(context.getName());
        final ServiceController<?> controller = context.getServiceRegistry().getService(serviceName);
        if(controller != null) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    private class DeploymentTrackerService implements Service<Object>{
        private final ArquillianConfig arqConfig;
        private final InjectedValue<ArquillianService> injectedArquillianService = new InjectedValue<ArquillianService>();

        public DeploymentTrackerService(ArquillianConfig arqConfig) {
            this.arqConfig = arqConfig;
        }

        @Override
        public void start(StartContext context) throws StartException {
            ArquillianService service = injectedArquillianService.getValue();
            service.registerDeployment(arqConfig);
        }

        @Override
        public void stop(StopContext context) {
            ArquillianService service = injectedArquillianService.getValue();
            service.unregisterDeployment(arqConfig);
        }

        @Override
        public Object getValue() throws IllegalStateException {
            return null;
        }
    }
}
