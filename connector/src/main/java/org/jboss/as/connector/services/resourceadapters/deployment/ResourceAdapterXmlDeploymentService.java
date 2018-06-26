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

import javax.security.auth.Subject;
import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.security.ElytronSubjectFactory;
import org.jboss.as.connector.services.resourceadapters.ResourceAdapterService;
import org.jboss.as.connector.subsystems.resourceadapters.ModifiableResourceAdapter;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.WritableServiceBasedNamingStore;
import org.jboss.jca.common.api.metadata.common.SecurityMetadata;
import org.jboss.jca.common.api.metadata.resourceadapter.Activation;
import org.jboss.jca.common.api.metadata.spec.Connector;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.core.security.picketbox.PicketBoxSubjectFactory;
import org.jboss.jca.deployers.DeployersLogger;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.Service;
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

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, "org.jboss.as.connector.deployers.RaXmlDeployer");

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
            context.getChildTarget()
                .addService(raServiceName,
                        new ResourceAdapterService(raServiceName, value.getDeployment().getResourceAdapter()))
                .addDependency(deploymentServiceName)
                .setInitialMode(ServiceController.Mode.ACTIVE).install();
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

        @Override
        protected org.jboss.jca.core.spi.security.SubjectFactory getSubjectFactory(
                SecurityMetadata securityMetadata, final String jndiName) throws DeployException {
            if (securityMetadata == null)
                return null;
            final String securityDomain = securityMetadata.resolveSecurityDomain();
            if (securityMetadata instanceof org.jboss.as.connector.metadata.api.common.SecurityMetadata &&
                    ((org.jboss.as.connector.metadata.api.common.SecurityMetadata)securityMetadata).isElytronEnabled()) {
                try {
                    return new ElytronSubjectFactory(null, new URI(jndiName));
                } catch (URISyntaxException e) {
                    throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(e);
                }
            } else if (securityDomain == null || securityDomain.trim().equals("")) {
                return null;
            } else if (((ModifiableResourceAdapter) raxml).getSubjectFactory() != null) {
                return new PicketBoxSubjectFactory(((ModifiableResourceAdapter) raxml).getSubjectFactory()){

                    @Override
                    public Subject createSubject(final String sd) {
                        ServerSecurityManager sm = ((ModifiableResourceAdapter) raxml).getSecManager();
                        if (sm != null) {
                            sm.push(sd);
                        }
                        try {
                            return super.createSubject(sd);
                        } finally {
                            if (sm != null) {
                                sm.pop();
                            }
                        }
                    }
                };
            }
            return null;
        }
    }
}
