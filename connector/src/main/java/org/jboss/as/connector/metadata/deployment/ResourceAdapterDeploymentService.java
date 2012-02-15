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
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.AdminObject;
import org.jboss.jca.common.api.metadata.ra.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.ra.Connector.Version;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.api.metadata.ra.ra10.ResourceAdapter10;
import org.jboss.jca.deployers.DeployersLogger;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.jboss.as.connector.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, "org.jboss.as.connector.deployers.RADeployer");

    private final Module module;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;
    private final Connector cmd;
    private final IronJacamar ijmd;
    private CommonDeployment raDeployment = null;

    private String raName;
    private ServiceName deploymentServiceName;

    public ResourceAdapterDeploymentService(final ConnectorXmlDescriptor connectorXmlDescriptor, final Connector cmd,
                                            final IronJacamar ijmd, final Module module, final ServiceName deploymentServiceName) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        this.cmd = cmd;
        this.ijmd = ijmd;
        this.module = module;
        this.raName = null;
        this.deploymentServiceName = deploymentServiceName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ServiceContainer container = context.getController().getServiceContainer();
        final URL url = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getUrl();
        final String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();
        final File root = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getRoot();
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("DEPLOYMENT name = %s",deploymentName);
        final AS7RaDeployer raDeployer =
            new AS7RaDeployer(context.getChildTarget(), url, deploymentName, root, module.getClassLoader(), cmd, ijmd);
        raDeployer.setConfiguration(config.getValue());

        ClassLoader old = SecurityActions.getThreadContextClassLoader();
        try {
            SecurityActions.setThreadContextClassLoader(module.getClassLoader());
            raDeployment = raDeployer.doDeploy();
        } catch (Throwable t) {
            unregisterAll(deploymentName);
            throw MESSAGES.failedToStartRaDeployment(t, deploymentName);
        } finally {
            SecurityActions.setThreadContextClassLoader(old);
        }

        value = new ResourceAdapterDeployment(raDeployment);

        managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());
        raName = value.getDeployment().getDeploymentName();

        if (raDeployer.checkActivation(cmd, ijmd)) {
            registry.getValue().registerResourceAdapterDeployment(value);

            ServiceName raServiceName = ConnectorServices.registerResourceAdapter(raName);
            context.getChildTarget()
                    .addService(raServiceName,
                                new ResourceAdapterService(raName, raServiceName, value.getDeployment().getResourceAdapter())).setInitialMode(Mode.ACTIVE)
                    .install();
        }
    }


    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        String deploymentName = value.getDeployment() != null ? value.getDeployment().getDeploymentName() : "";
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("Stopping sevice %s",
                        ConnectorServices.RESOURCE_ADAPTER_DEPLOYMENT_SERVICE_PREFIX.append(deploymentName));

        unregisterAll(deploymentName);
    }

    @Override
    public void unregisterAll(String deploymentName) {

        if (raName != null && deploymentServiceName != null) {
            ConnectorServices.unregisterDeployment(raName, deploymentServiceName);
        }

        if (raName != null) {
            ConnectorServices.unregisterResourceAdapterIdentifier(raName);
        }


        super.unregisterAll(deploymentName);
    }

    public CommonDeployment getRaDeployment() {
        return raDeployment;
    }

    private class AS7RaDeployer extends AbstractAS7RaDeployer {

        private final IronJacamar ijmd;

        public AS7RaDeployer(ServiceTarget serviceContainer, URL url, String deploymentName, File root, ClassLoader cl,
                Connector cmd, IronJacamar ijmd) {
            super(serviceContainer, url, deploymentName, root, cl, cmd);
            this.ijmd = ijmd;
        }

        @Override
        public CommonDeployment doDeploy() throws Throwable {

            this.setConfiguration(getConfig().getValue());

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, cl, cmd, ijmd);

            return dep;
        }

        @Override
        protected boolean checkActivation(Connector cmd, IronJacamar ijmd) {
            if (cmd != null) {
                Set<String> raMcfClasses = new HashSet<String>();
                Set<String> raAoClasses = new HashSet<String>();

                if (cmd.getVersion() == Version.V_10) {
                    ResourceAdapter10 ra10 = (ResourceAdapter10) cmd.getResourceadapter();
                    raMcfClasses.add(ra10.getManagedConnectionFactoryClass().getValue());
                } else {
                    ResourceAdapter1516 ra = (ResourceAdapter1516) cmd.getResourceadapter();
                    if (ra != null && ra.getOutboundResourceadapter() != null &&
                        ra.getOutboundResourceadapter().getConnectionDefinitions() != null) {
                        List<ConnectionDefinition> cdMetas = ra.getOutboundResourceadapter().getConnectionDefinitions();
                        if (cdMetas.size() > 0) {
                            for (ConnectionDefinition cdMeta : cdMetas) {
                                raMcfClasses.add(cdMeta.getManagedConnectionFactoryClass().getValue());
                            }
                        }
                    }

                    if (ra != null && ra.getAdminObjects() != null) {
                        List<AdminObject> aoMetas = ra.getAdminObjects();
                        if (aoMetas.size() > 0) {
                            for (AdminObject aoMeta : aoMetas) {
                                raAoClasses.add(aoMeta.getAdminobjectClass().getValue());
                            }
                        }
                    }

                    // Pure inflow
                    if (raMcfClasses.size() == 0 && raAoClasses.size() == 0)
                        return true;
                }

                if (ijmd != null) {
                    if (ijmd.getConnectionDefinitions() != null) {
                        for (org.jboss.jca.common.api.metadata.common.CommonConnDef def : ijmd.getConnectionDefinitions()) {
                            String clz = def.getClassName();

                            if (raMcfClasses.contains(clz))
                                return true;
                        }
                    }

                    if (ijmd.getAdminObjects() != null) {
                        for (org.jboss.jca.common.api.metadata.common.CommonAdminObject def : ijmd.getAdminObjects()) {
                            String clz = def.getClassName();

                            if (raAoClasses.contains(clz))
                                return true;
                        }
                    }
                }
            }

            return false;
        }

        @Override
        protected DeployersLogger getLogger() {
            return DEPLOYERS_LOGGER;
        }
    }

}
