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

import java.io.File;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.mdr.AS7MetadataRepository;
import org.jboss.as.connector.services.resourceadapters.IronJacamarActivationResourceService;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterService;
import org.jboss.as.connector.subsystems.resourceadapters.RaOperationUtil;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.spec.AdminObject;
import org.jboss.jca.common.api.metadata.spec.ConnectionDefinition;
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
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A ResourceAdapterDeploymentService.
 *
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, "org.jboss.as.connector.deployers.RADeployer");

    private final ClassLoader classLoader;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;
    private final Connector cmd;
    private final Activation activation;
    private final ManagementResourceRegistration registration;
    private final Resource deploymentResource;
    private CommonDeployment raDeployment = null;
    private String deploymentName;

    private ServiceName deploymentServiceName;
    private final ServiceName duServiceName;

    /**
     * @param connectorXmlDescriptor
     * @param cmd
     * @param classLoader
     * @param deploymentServiceName
     * @param duServiceName          the deployment unit's service name
     * @activationm activation
     */
    public ResourceAdapterDeploymentService(final ConnectorXmlDescriptor connectorXmlDescriptor, final Connector cmd,
                                            final Activation activation, final ClassLoader classLoader, final ServiceName deploymentServiceName, final ServiceName duServiceName,
                                            final ManagementResourceRegistration registration, final Resource deploymentResource) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        this.cmd = cmd;
        this.activation = activation;
        this.classLoader = classLoader;
        this.deploymentServiceName = deploymentServiceName;
        this.duServiceName = duServiceName;
        this.registration = registration;
        this.deploymentResource = deploymentResource;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final URL url = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getUrl();
        deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();
        connectorServicesRegistrationName = deploymentName;
        final File root = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getRoot();
        DEPLOYMENT_CONNECTOR_LOGGER.debugf("DEPLOYMENT name = %s", deploymentName);
        final boolean fromModule = duServiceName.getParent().equals(RaOperationUtil.RAR_MODULE);
        final WildFLyRaDeployer raDeployer =
                new WildFLyRaDeployer(context.getChildTarget(), url, deploymentName, root, classLoader, cmd, activation, deploymentServiceName, fromModule);
        raDeployer.setConfiguration(config.getValue());

        ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
        try {
            try {
                WritableServiceBasedNamingStore.pushOwner(duServiceName);
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(classLoader);
                raDeployment = raDeployer.doDeploy();
                deploymentName = raDeployment.getDeploymentName();
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
                WritableServiceBasedNamingStore.popOwner();
            }
            if (raDeployer.checkActivation(cmd, activation)) {
                DEPLOYMENT_CONNECTOR_LOGGER.debugf("Activating: %s", deploymentName);
                ServiceName raServiceName = ConnectorServices.getResourceAdapterServiceName(deploymentName);
                value = new ResourceAdapterDeployment(raDeployment, deploymentName, raServiceName);
                managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());
                registry.getValue().registerResourceAdapterDeployment(value);
                ServiceTarget serviceTarget = context.getChildTarget();
                serviceTarget
                        .addService(raServiceName,
                                new ResourceAdapterService(raServiceName, value.getDeployment().getResourceAdapter())).setInitialMode(Mode.ACTIVE)
                        .install();
                final ServiceName deployerServiceName = ConnectorServices.RESOURCE_ADAPTER_DEPLOYER_SERVICE_PREFIX.append(connectorXmlDescriptor.getDeploymentName());

                IronJacamarActivationResourceService ijResourceService = new IronJacamarActivationResourceService(registration, deploymentResource, false);

                serviceTarget.addService(deployerServiceName.append(ConnectorServices.IRONJACAMAR_RESOURCE), ijResourceService)
                        .addDependency(ConnectorServices.IRONJACAMAR_MDR, AS7MetadataRepository.class, ijResourceService.getMdrInjector())
                        .addDependency(deployerServiceName, ResourceAdapterDeployment.class, ijResourceService.getResourceAdapterDeploymentInjector())
                        .setInitialMode(Mode.PASSIVE)
                        .install();

            } else {
                DEPLOYMENT_CONNECTOR_LOGGER.debugf("Not activating: %s", deploymentName);
            }
        } catch (Throwable t) {
            cleanupStartAsync(context, deploymentName, t, duServiceName, classLoader);
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
                        context.failed(ConnectorLogger.ROOT_LOGGER.failedToStartRaDeployment(cause, deploymentName));
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
        stopAsync(context, deploymentName, deploymentServiceName);
    }

    public CommonDeployment getRaDeployment() {
        return raDeployment;
    }

    public AS7MetadataRepository getMdr() {
        return mdr.getValue();
    }

    private class WildFLyRaDeployer extends AbstractWildFlyRaDeployer {

        private final Activation activation;
        private final boolean fromModule;

        public WildFLyRaDeployer(ServiceTarget serviceContainer, URL url, String deploymentName, File root, ClassLoader cl,
                                 Connector cmd, Activation activation, final ServiceName deploymentServiceName, final boolean fromModule) {
            super(serviceContainer, url, deploymentName, root, cl, cmd, deploymentServiceName);
            this.activation = activation;
            this.fromModule = fromModule;
        }

        @Override
        public CommonDeployment doDeploy() throws Throwable {

            this.setConfiguration(getConfig().getValue());

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, cl, cmd, activation);

            return dep;
        }

        @Override
        protected boolean checkActivation(Connector cmd, Activation activation) {
            if (cmd != null) {
                Set<String> raMcfClasses = new HashSet<String>();
                Set<String> raAoClasses = new HashSet<String>();

                ResourceAdapter ra = cmd.getResourceadapter();
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
                if (raMcfClasses.size() == 0 && raAoClasses.size() == 0 && !fromModule)
                    return true;


                if (activation != null) {
                    if (activation.getConnectionDefinitions() != null) {
                        for (org.jboss.jca.common.api.metadata.resourceadapter.ConnectionDefinition def : activation.getConnectionDefinitions()) {
                            String clz = def.getClassName();

                            if (raMcfClasses.contains(clz))
                                return true;
                        }
                    }

                    if (activation.getAdminObjects() != null) {
                        for (org.jboss.jca.common.api.metadata.resourceadapter.AdminObject def : activation.getAdminObjects()) {
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
