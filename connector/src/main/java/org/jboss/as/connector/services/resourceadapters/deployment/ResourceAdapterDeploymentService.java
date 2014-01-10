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

package org.jboss.as.connector.services.resourceadapters.deployment;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;
import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterService;
import org.jboss.as.connector.subsystems.resourceadapters.RaOperationUtil;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
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
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, "org.jboss.as.connector.deployers.RADeployer");

    private final ClassLoader classLoader;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;
    private final Connector cmd;
    private final IronJacamar ijmd;
    private CommonDeployment raDeployment = null;
    private String deploymentName;

    private ServiceName deploymentServiceName;
    private final ServiceName duServiceName;

    /**
     *
     * @param connectorXmlDescriptor
     * @param cmd
     * @param ijmd
     * @param classLoader
     * @param deploymentServiceName
     * @param duServiceName the deployment unit's service name
     */
    public ResourceAdapterDeploymentService(final ConnectorXmlDescriptor connectorXmlDescriptor, final Connector cmd,
                                            final IronJacamar ijmd, final ClassLoader classLoader, final ServiceName deploymentServiceName, final ServiceName duServiceName) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        this.cmd = cmd;
        this.ijmd = ijmd;
        this.classLoader = classLoader;
        this.deploymentServiceName = deploymentServiceName;
        this.duServiceName = duServiceName;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final URL url = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getUrl();
        deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();
        final File root = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getRoot();
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("DEPLOYMENT name = %s",deploymentName);
        final boolean fromModule = duServiceName.getParent().equals(RaOperationUtil.RAR_MODULE);
        final AS7RaDeployer raDeployer =
            new AS7RaDeployer(context.getChildTarget(), url, deploymentName, root, classLoader, cmd, ijmd, deploymentServiceName, fromModule);
        raDeployer.setConfiguration(config.getValue());

        ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            WritableServiceBasedNamingStore.pushOwner(duServiceName);
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
            raDeployment = raDeployer.doDeploy();
            deploymentName = raDeployment.getDeploymentName();
        } catch (Throwable t) {
            // To clean up we need to invoke blocking behavior, so do that in another thread
            // and let this MSC thread return
            cleanupStartAsync(context, deploymentName, t, duServiceName, classLoader);
            return;
        } finally {
            WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
            WritableServiceBasedNamingStore.popOwner();
        }


        if (raDeployer.checkActivation(cmd, ijmd)) {
            DEPLOYMENT_CONNECTOR_LOGGER.debugf("Activating: %s", deploymentName);

            ServiceName raServiceName = ConnectorServices.getResourceAdapterServiceName(deploymentName);
            value = new ResourceAdapterDeployment(raDeployment, deploymentName, raServiceName);

            managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());
            registry.getValue().registerResourceAdapterDeployment(value);

            context.getChildTarget()
                    .addService(raServiceName,
                                new ResourceAdapterService(deploymentName, raServiceName, value.getDeployment().getResourceAdapter())).setInitialMode(Mode.ACTIVE)
                    .install();
        } else {
            DEPLOYMENT_CONNECTOR_LOGGER.debugf("Not activating: %s", deploymentName);
        }
    }

    // TODO this could be replaced by the superclass method if there is no need for the TCCL change and push/pop owner
    // The stop() call doesn't do that so it's probably not needed
    private void cleanupStartAsync(final StartContext context, final String deploymentName, final Throwable cause,
            final ServiceName duServiceName, final ClassLoader toUse) {
        ExecutorService executorService = getLifecycleExecutorService();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
                try {
                    WritableServiceBasedNamingStore.pushOwner(duServiceName);
                    WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(toUse);
                    unregisterAll(deploymentName);
                } finally {
                    try {
                        context.failed(MESSAGES.failedToStartRaDeployment(cause, deploymentName));
                    } finally {
                        WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
                        WritableServiceBasedNamingStore.popOwner();
                    }
                }
            }
        };
        try {
            executorService.execute(r);
        } catch (RejectedExecutionException e) {
            r.run();
        } finally {
            context.asynchronous();
        }
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        stopAsync(context, deploymentName, deploymentServiceName);    }

    @Override
    public void unregisterAll(String deploymentName) {

        ConnectorServices.unregisterResourceAdapterIdentifier(deploymentName);

        super.unregisterAll(deploymentName);

        if (mdr != null && mdr.getValue() != null && deploymentName != null) {
            try {
                mdr.getValue().unregisterResourceAdapter(deploymentName);
            } catch (Throwable t) {
                DEPLOYMENT_CONNECTOR_LOGGER.debug("Exception during unregistering deployment", t);
            }
        }
    }

    public CommonDeployment getRaDeployment() {
        return raDeployment;
    }

    public AS7MetadataRepository getMdr() {
            return mdr.getValue();
        }

    private class AS7RaDeployer extends AbstractAS7RaDeployer {

        private final IronJacamar ijmd;
        private final boolean fromModule;

        public AS7RaDeployer(ServiceTarget serviceContainer, URL url, String deploymentName, File root, ClassLoader cl,
                Connector cmd, IronJacamar ijmd,  final ServiceName deploymentServiceName, final boolean fromModule) {
            super(serviceContainer, url, deploymentName, root, cl, cmd, deploymentServiceName);
            this.ijmd = ijmd;
            this.fromModule = fromModule;
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

                    // Pure inflow always active except in case it is deployed as module
                    if (raMcfClasses.size() == 0 && raAoClasses.size() == 0 && ! fromModule)
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
