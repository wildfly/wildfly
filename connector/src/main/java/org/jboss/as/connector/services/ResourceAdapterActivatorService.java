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

package org.jboss.as.connector.services;

import static org.jboss.as.connector.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;
import static org.jboss.as.connector.ConnectorMessages.MESSAGES;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.deployment.AbstractResourceAdapterDeploymentService;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterActivatorService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, ResourceAdapterActivator.class.getName());

    private final ClassLoader cl;
    private final Connector cmd;
    private final IronJacamar ijmd;
    private final String deploymentName;

    private CommonDeployment deploymentMD;

    public ResourceAdapterActivatorService(final Connector cmd, final IronJacamar ijmd, ClassLoader cl,
            final String deploymentName) {
        this.cmd = cmd;
        this.ijmd = ijmd;
        this.cl = cl;
        this.deploymentName = deploymentName;
    }

    @Override
    public void start(StartContext context) throws StartException {

        String pathname = "file://RaActivator" + deploymentName;

        try {
            ResourceAdapterActivator activator = new ResourceAdapterActivator(context.getChildTarget(), new URL(pathname), deploymentName,
                    new File(pathname), cl, cmd, ijmd);
            activator.setConfiguration(getConfig().getValue());
            // FIXME!!, this should probably be done by IJ and not the service
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
               Thread.currentThread().setContextClassLoader(cl);
               deploymentMD = activator.doDeploy();
            } finally {
               Thread.currentThread().setContextClassLoader(old);
            }
        } catch (Throwable e) {
            unregisterAll(deploymentName);
            throw MESSAGES.failedToStartRaDeployment(e, deploymentName);
        }

        value = new ResourceAdapterDeployment(deploymentMD);
        registry.getValue().registerResourceAdapterDeployment(value);
        managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());

        String raName = value.getDeployment().getDeploymentName();
        ServiceName raServiceName = ConnectorServices.registerResourceAdapter(raName);

        context.getChildTarget()
                .addService(raServiceName,
                    new ResourceAdapterService(raName, raServiceName,
                                               value.getDeployment().getResourceAdapter())).setInitialMode(Mode.ACTIVE)
                .install();
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("Started service %s", ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE);
    }

    /**
     * Stop
     */
    @Override
        public void stop(StopContext context) {
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("Stopping service %s", ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE);
        unregisterAll(deploymentName);
    }
    @Override
    public void unregisterAll(String deploymentName) {
        super.unregisterAll(deploymentName);

    }

    public CommonDeployment getDeploymentMD() {
        return deploymentMD;
    }

    private class ResourceAdapterActivator extends AbstractAS7RaDeployer {

        private final IronJacamar ijmd;

        public ResourceAdapterActivator(ServiceTarget serviceTarget, URL url, String deploymentName, File root,
                ClassLoader cl, Connector cmd, IronJacamar ijmd) {
            super(serviceTarget, url, deploymentName, root, cl, cmd);
            this.ijmd = ijmd;
        }

        @Override
        public CommonDeployment doDeploy() throws Throwable {

            this.setConfiguration(getConfig().getValue());
            //never validate bean for services activated in this way (JMS)
            this.getConfiguration().setBeanValidation(false);

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
                    if (ra != null && ra.getOutboundResourceadapter() != null
                            && ra.getOutboundResourceadapter().getConnectionDefinitions() != null) {
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
                    Set<String> ijMcfClasses = new HashSet<String>();
                    Set<String> ijAoClasses = new HashSet<String>();

                    boolean mcfSingle = false;
                    boolean aoSingle = false;

                    boolean mcfOk = true;
                    boolean aoOk = true;

                    if (ijmd.getConnectionDefinitions() != null) {
                        for (org.jboss.jca.common.api.metadata.common.CommonConnDef def : ijmd.getConnectionDefinitions()) {
                            String clz = def.getClassName();

                            if (clz == null) {
                                if (raMcfClasses.size() == 1) {
                                    mcfSingle = true;
                                }
                            } else {
                                ijMcfClasses.add(clz);
                            }
                        }
                    }

                    if (!mcfSingle) {
                        Iterator<String> it = raMcfClasses.iterator();
                        while (mcfOk && it.hasNext()) {
                            String clz = it.next();
                            if (!ijMcfClasses.contains(clz))
                                mcfOk = false;
                        }
                    }

                    if (ijmd.getAdminObjects() != null) {
                        for (org.jboss.jca.common.api.metadata.common.CommonAdminObject def : ijmd.getAdminObjects()) {
                            String clz = def.getClassName();
                            if (clz == null) {
                                if (raAoClasses.size() == 1) {
                                    aoSingle = true;
                                }
                            } else {
                                ijAoClasses.add(clz);
                            }
                        }
                    }

                    if (!aoSingle) {
                        Iterator<String> it = raAoClasses.iterator();
                        while (aoOk && it.hasNext()) {
                            String clz = it.next();
                            if (!ijAoClasses.contains(clz))
                                aoOk = false;
                        }
                    }

                    return mcfOk && aoOk;
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
