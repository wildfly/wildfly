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

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.ResourceAdapterService;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.deployers.DeployersLogger;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;
import java.net.URL;

import static org.jboss.as.connector.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;

/**
 * A ResourceAdapterXmlDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterXmlDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, "org.jboss.as.connector.deployers.RaXmlDeployer");

    private final Module module;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;
    private final ResourceAdapter raxml;
    private final String deployment;

    private String raName;
    private ServiceName deploymentServiceName;
    private CommonDeployment raxmlDeployment = null;

    public ResourceAdapterXmlDeploymentService(ConnectorXmlDescriptor connectorXmlDescriptor, ResourceAdapter raxml,
                                               Module module, final String deployment, final ServiceName deploymentServiceName) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        this.raxml = raxml;
        this.module = module;
        this.deployment = deployment;
        this.raName = raxml.getArchive().substring(0, raxml.getArchive().indexOf(".rar"));
        this.deploymentServiceName = deploymentServiceName;
    }

    /**
     * Start
     */
    @Override
    public void start(StartContext context) throws StartException {
        try {
            Connector cmd = mdr.getValue().getResourceAdapter(deployment);
            IronJacamar ijmd = mdr.getValue().getIronJacamar(deployment);
            File root = mdr.getValue().getRoot(deployment);

            cmd = (new Merger()).mergeConnectorWithCommonIronJacamar(raxml, cmd);

            final ServiceContainer container = context.getController().getServiceContainer();
            final AS7RaXmlDeployer raDeployer = new AS7RaXmlDeployer(context.getChildTarget(), connectorXmlDescriptor.getUrl(),
                raName, root, module.getClassLoader(), cmd, raxml, ijmd);

            raDeployer.setConfiguration(config.getValue());


            try {
                WritableServiceBasedNamingStore.pushOwner(container.subTarget());
                try {
                    raxmlDeployment = raDeployer.doDeploy();
                } finally {
                    WritableServiceBasedNamingStore.popOwner();
                }
            } catch (Throwable t) {
                throw MESSAGES.failedToStartRaDeployment(t, raName);
            }

            value = new ResourceAdapterDeployment(raxmlDeployment);
            managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());

            registry.getValue().registerResourceAdapterDeployment(value);

            ServiceName raServiceName = ConnectorServices.registerResourceAdapter(raName);

            context.getChildTarget()
                    .addService(raServiceName,
                            new ResourceAdapterService(raName, raServiceName, value.getDeployment().getResourceAdapter()))
                    .addDependency(deploymentServiceName)
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("Stopping service %s",
            ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(this.value.getDeployment().getDeploymentName()));

        if (raName != null && deploymentServiceName != null) {
            ConnectorServices.unregisterDeployment(raName, deploymentServiceName);
        }

        if (raName != null) {
            ConnectorServices.unregisterResourceAdapterIdentifier(raName);
        }

        managementRepository.getValue().getConnectors().remove(value.getDeployment().getConnector());
        super.stop(context);
    }

    public CommonDeployment getRaxmlDeployment() {
        return raxmlDeployment;
    }

    private class AS7RaXmlDeployer extends AbstractAS7RaDeployer {

        private final ResourceAdapter ra;
        private final IronJacamar ijmd;

        public AS7RaXmlDeployer(ServiceTarget serviceTarget, URL url, String deploymentName, File root, ClassLoader cl,
                Connector cmd, ResourceAdapter ra, IronJacamar ijmd) {
            super(serviceTarget, url, deploymentName, root, cl, cmd);
            this.ra = ra;
            this.ijmd = ijmd;
        }

        @Override
        public CommonDeployment doDeploy() throws Throwable {

            this.setConfiguration(getConfig().getValue());

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, cl, cmd, ijmd, ra);

            return dep;
        }

        @Override
        protected boolean checkActivation(Connector cmd, IronJacamar ijmd) {
            return true;
        }

        @Override
        protected DeployersLogger getLogger() {
            return DEPLOYERS_LOGGER;
        }

    }
}
