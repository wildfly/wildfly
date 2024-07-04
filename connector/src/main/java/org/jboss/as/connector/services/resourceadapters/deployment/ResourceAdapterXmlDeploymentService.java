/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.services.resourceadapters.deployment;

import static org.jboss.as.connector.logging.ConnectorLogger.DEPLOYMENT_CONNECTOR_LOGGER;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterService;
import org.jboss.as.connector.subsystems.resourceadapters.ModifiableResourceAdapter;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.deployers.DeployersLogger;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.security.manager.WildFlySecurityManager;

/**
 * A ResourceAdapterXmlDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterXmlDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(MethodHandles.lookup(), DeployersLogger.class, "org.jboss.as.connector.deployers.RaXmlDeployer");

    private final Module module;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;

    private Activation raxml;
    private final String deployment;

    private String raName;
    private ServiceName deploymentServiceName;
    private CommonDeployment raxmlDeployment = null;
    private final ServiceName duServiceName;

    public ResourceAdapterXmlDeploymentService(ConnectorXmlDescriptor connectorXmlDescriptor, Activation raxml,
                                               Module module, final String deployment, final ServiceName deploymentServiceName, final ServiceName duServiceName) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        synchronized (this) {
            this.raxml = raxml;
        }
        this.module = module;
        this.deployment = deployment;
        this.raName = deployment;
        this.deploymentServiceName = deploymentServiceName;
        this.duServiceName = duServiceName;
    }

    /**
     * Start
     */
    @Override
    public void start(StartContext context) throws StartException {
        try {
            Connector cmd = mdr.getValue().getResourceAdapter(deployment);
            File root = mdr.getValue().getRoot(deployment);

            Activation localRaXml = getRaxml();
            cmd = (new Merger()).mergeConnectorWithCommonIronJacamar(localRaXml, cmd);

            String id = ((ModifiableResourceAdapter) raxml).getId();
            final ServiceName raServiceName;
            if (id == null || id.trim().isEmpty()) {
                raServiceName = ConnectorServices.getResourceAdapterServiceName(raName);
                this.connectorServicesRegistrationName = raName;
            } else {
                raServiceName = ConnectorServices.getResourceAdapterServiceName(id);
                this.connectorServicesRegistrationName = id;
            }
            final WildFlyRaXmlDeployer raDeployer = new WildFlyRaXmlDeployer(context.getChildTarget(), connectorXmlDescriptor.getUrl(),
                raName, root, module.getClassLoader(), cmd, localRaXml, deploymentServiceName);

            raDeployer.setConfiguration(config.getValue());

            WritableServiceBasedNamingStore.pushOwner(duServiceName);
            ClassLoader old = WildFlySecurityManager.getCurrentContextClassLoaderPrivileged();
            try {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(module.getClassLoader());
                raxmlDeployment = raDeployer.doDeploy();
            } finally {
                WildFlySecurityManager.setCurrentContextClassLoaderPrivileged(old);
                WritableServiceBasedNamingStore.popOwner();
            }
            value = new ResourceAdapterDeployment(raxmlDeployment, raName, raServiceName);
            managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());
            registry.getValue().registerResourceAdapterDeployment(value);
            final ServiceBuilder raServiceSB = context.getChildTarget()
                .addService(raServiceName,
                        new ResourceAdapterService(raServiceName, value.getDeployment().getResourceAdapter()));
            raServiceSB.requires(deploymentServiceName);
            raServiceSB.setInitialMode(ServiceController.Mode.ACTIVE).install();
        } catch (Throwable t) {
            cleanupStartAsync(context, raName, deploymentServiceName, t);
        }
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        stopAsync(context, raName, deploymentServiceName);
    }

    @Override
    public Collection<String> getJndiAliases() {
        return Collections.emptyList();
    }

    public CommonDeployment getRaxmlDeployment() {
        return raxmlDeployment;
    }

    public synchronized void setRaxml(Activation raxml) {
            this.raxml = raxml;
    }

    public synchronized Activation getRaxml() {
            return raxml;
    }


    private class WildFlyRaXmlDeployer extends AbstractWildFlyRaDeployer {

        private final Activation activation;


        public WildFlyRaXmlDeployer(ServiceTarget serviceTarget, URL url, String deploymentName, File root, ClassLoader cl,
                                    Connector cmd, Activation activation, final ServiceName deploymentServiceName) {
            super(serviceTarget, url, deploymentName, root, cl, cmd, deploymentServiceName);
            this.activation = activation;
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
            return true;
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
