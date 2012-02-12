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

package org.jboss.as.connector.metadata.deployment;

import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.jca.deployers.DeployersLogger;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import static org.jboss.as.connector.ConnectorLogger.ROOT_LOGGER;

/**
 * A ResourceAdapterXmlDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class InactiveResourceAdapterDeploymentService implements
        Service<InactiveResourceAdapterDeploymentService.InactiveResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, "org.jboss.as.connector.deployers.RaXmlInactiveDeployer");

    private final InactiveResourceAdapterDeployment value;

    public InactiveResourceAdapterDeploymentService(ConnectorXmlDescriptor connectorXmlDescriptor,
                                                    Module module, final String deployment, final String deploymentUnitName, final ManagementResourceRegistration registration) {
        this.value = new InactiveResourceAdapterDeployment(connectorXmlDescriptor, module, deployment, deploymentUnitName, registration);
    }


    public InactiveResourceAdapterDeployment getValue() {
        return value;
    }

    /**
     * Start
     */
    @Override
    public void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("starting Inactive:" + value.toString());
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {

    }

    public static class InactiveResourceAdapterDeployment  {
        private final Module module;
        private final ConnectorXmlDescriptor connectorXmlDescriptor;
        private final String deployment;
        private final String deploymentUnitName;
        private final ManagementResourceRegistration registration;


        public InactiveResourceAdapterDeployment(final ConnectorXmlDescriptor connectorXmlDescriptor, final Module module, final String deployment,
                                                 final String deploymentUnitName, final ManagementResourceRegistration registration) {
            this.connectorXmlDescriptor = connectorXmlDescriptor;
            this.module = module;
            this.deployment = deployment;
            this.deploymentUnitName = deploymentUnitName;
            this.registration = registration;
        }

        public Module getModule() {
            return module;
        }

        public ConnectorXmlDescriptor getConnectorXmlDescriptor() {
            return connectorXmlDescriptor;
        }

        public String getDeployment() {
            return deployment;
        }

        public String getDeploymentUnitName() {
            return deploymentUnitName;
        }

        public ManagementResourceRegistration getRegistration() {
            return registration;
        }

        @Override
        public String toString() {
            return "InactiveResourceAdapterDeployment{" +
                    "module=" + module +
                    ", connectorXmlDescriptor=" + connectorXmlDescriptor +
                    ", deployment='" + deployment + '\'' +
                    ", deploymentUnitName='" + deploymentUnitName + '\'' +
                    ", registration=" + registration +
                    '}';
        }
    }
}
