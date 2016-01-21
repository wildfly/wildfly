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

package org.jboss.as.connector.services.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.services.resourceadapters.deployment.AbstractResourceAdapterDeploymentService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.resourceadapter.AdminObject;
import org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.api.metadata.spec.ResourceAdapter;
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
    private final Activation activation;
    private final String deploymentName;

    private CommonDeployment deploymentMD;
    private ContextNames.BindInfo bindInfo;
    private boolean createBinderService = true;

    public ResourceAdapterActivatorService(final Connector cmd, final Activation activation, ClassLoader cl,
            final String deploymentName) {
        this.cmd = cmd;
        this.activation = activation;
        this.cl = cl;
        this.deploymentName = deploymentName;
        this.connectorServicesRegistrationName = deploymentName;
        this.bindInfo = null;
    }

    public ContextNames.BindInfo getBindInfo(String jndi) {
        if (bindInfo != null)
            return bindInfo;

        return ContextNames.bindInfoFor(jndi);
    }

    public void setBindInfo(ContextNames.BindInfo bindInfo) {
        this.bindInfo = bindInfo;
    }

    public boolean isCreateBinderService() {
        return createBinderService;
    }

    public void setCreateBinderService(boolean createBinderService) {
        this.createBinderService = createBinderService;
    }

    @Override
    public void start(StartContext context) throws StartException {

        String pathname = "file://RaActivator" + deploymentName;

        try {
            ResourceAdapterActivator activator = new ResourceAdapterActivator(context.getChildTarget(), new URL(pathname), deploymentName,
                    new File(pathname), cl, cmd, activation);
            activator.setConfiguration(getConfig().getValue());
            // FIXME!!, this should probably be done by IJ and not the service
            ClassLoader old = Thread.currentThread().getContextClassLoader();
            try {
               Thread.currentThread().setContextClassLoader(cl);
               deploymentMD = activator.doDeploy();
            } finally {
               Thread.currentThread().setContextClassLoader(old);
            }
            String raName = deploymentMD.getDeploymentName();
            ServiceName raServiceName = ConnectorServices.getResourceAdapterServiceName(raName);
            value = new ResourceAdapterDeployment(deploymentMD, raName, raServiceName);
            registry.getValue().registerResourceAdapterDeployment(value);
            managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());

            context.getChildTarget()
                    .addService(raServiceName,
                            new ResourceAdapterService(raServiceName, value.getDeployment().getResourceAdapter())).setInitialMode(Mode.ACTIVE)
                    .install();
            DEPLOYMENT_CONNECTOR_LOGGER.debugf("Started service %s", ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE);
        } catch (Throwable t) {
            // To clean up we need to invoke blocking behavior, so do that in another thread
            // and let this MSC thread return
            String raName = deploymentName;
            ServiceName raServiceName = ConnectorServices.getResourceAdapterServiceName(raName);
            cleanupStartAsync(context, deploymentName, raServiceName, t);
        }

    }

    /**
     * Stop
     */
    @Override
    public void stop(final StopContext context) {
        stopAsync(context, deploymentName, ConnectorServices.RESOURCE_ADAPTER_ACTIVATOR_SERVICE);
    }

    public CommonDeployment getDeploymentMD() {
        return deploymentMD;
    }

    private class ResourceAdapterActivator extends AbstractWildFlyRaDeployer {

        private final Activation activation;

        public ResourceAdapterActivator(ServiceTarget serviceTarget, URL url, String deploymentName, File root,
                ClassLoader cl, Connector cmd, Activation activation) {
            super(serviceTarget, url, deploymentName, root, cl, cmd, null);
            this.activation = activation;
        }

        @Override
        public CommonDeployment doDeploy() throws Throwable {

            this.setConfiguration(getConfig().getValue());
            //never validate bean for services activated in this way (JMS)
            this.getConfiguration().setBeanValidation(false);

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, cl, cmd, activation);

            return dep;
        }

        @Override
        protected boolean checkActivation(Connector cmd, Activation activation) {
            if (cmd != null) {
                Set<String> raMcfClasses = new HashSet<String>();
                Set<String> raAoClasses = new HashSet<String>();


                ResourceAdapter ra = (ResourceAdapter) cmd.getResourceadapter();
                if (ra != null && ra.getOutboundResourceadapter() != null
                        && ra.getOutboundResourceadapter().getConnectionDefinitions() != null) {
                    List<org.jboss.jca.common.api.metadata.spec.ConnectionDefinition> cdMetas = ra.getOutboundResourceadapter().getConnectionDefinitions();
                    if (cdMetas.size() > 0) {
                        for (org.jboss.jca.common.api.metadata.spec.ConnectionDefinition cdMeta : cdMetas) {
                            raMcfClasses.add(cdMeta.getManagedConnectionFactoryClass().getValue());
                        }
                    }
                }

                if (ra != null && ra.getAdminObjects() != null) {
                    List<org.jboss.jca.common.api.metadata.spec.AdminObject> aoMetas = ra.getAdminObjects();
                    if (aoMetas.size() > 0) {
                        for (org.jboss.jca.common.api.metadata.spec.AdminObject aoMeta : aoMetas) {
                            raAoClasses.add(aoMeta.getAdminobjectClass().getValue());
                        }
                    }
                }

                // Pure inflow
                if (raMcfClasses.size() == 0 && raAoClasses.size() == 0)
                    return true;


                if (activation != null) {
                    Set<String> ijMcfClasses = new HashSet<String>();
                    Set<String> ijAoClasses = new HashSet<String>();

                    boolean mcfSingle = raMcfClasses.size() == 1;
                    boolean aoSingle = raAoClasses.size() == 1;

                    boolean mcfOk = true;
                    boolean aoOk = true;

                    if (activation.getConnectionDefinitions() != null) {
                        for (ConnectionDefinition def : activation.getConnectionDefinitions()) {
                            String clz = def.getClassName();
                            if (clz != null) {
                                ijMcfClasses.add(clz);
                            }
                        }
                    }

                    if (!mcfSingle) {
                        Iterator<String> it = ijMcfClasses.iterator();
                        while (mcfOk && it.hasNext()) {
                            String clz = it.next();
                            if (!raMcfClasses.contains(clz))
                                mcfOk = false;
                        }
                    }

                    if (activation.getAdminObjects() != null) {
                        for (AdminObject def : activation.getAdminObjects()) {
                            String clz = def.getClassName();
                            if (clz != null) {
                                ijAoClasses.add(clz);
                            }
                        }
                    }

                    if (!aoSingle) {
                        Iterator<String> it = ijAoClasses.iterator();
                        while (aoOk && it.hasNext()) {
                            String clz = it.next();
                            if (!raAoClasses.contains(clz))
                                aoOk = false;
                        }
                    }

                    return mcfOk || aoOk;
                }
            }

            return false;
        }

        @Override
        protected DeployersLogger getLogger() {
            return DEPLOYERS_LOGGER;
        }

        @Override
        protected void setRecoveryForResourceAdapterInResourceAdapterRepository(String key, boolean isXA) {
            try {
                raRepository.getValue().setRecoveryForResourceAdapter(key, isXA);
            } catch (Throwable t) {
                DEPLOYMENT_CONNECTOR_LOGGER.unableToRegisterRecovery(key, isXA);
            }
        }
    }

}
