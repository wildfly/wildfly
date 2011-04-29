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

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.transaction.TransactionManager;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.AdminObjectReferenceFactoryService;
import org.jboss.as.connector.services.AdminObjectService;
import org.jboss.as.connector.services.ConnectionFactoryReferenceFactoryService;
import org.jboss.as.connector.services.ConnectionFactoryService;
import org.jboss.as.connector.subsystems.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.AbstractResourceAdapterDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;
import org.jboss.security.SubjectFactory;

import com.arjuna.ats.jbossatx.jta.TransactionManagerService;

/**
 * A ResourceAdapterXmlDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterXmlDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final Logger log = Logger.getLogger("org.jboss.as.deployment.connector");

    private final Module module;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;
    private final String deploymentName;
    private final File root;
    private final ResourceAdapter raxml;
    private final Connector cmd;
    private final IronJacamar ijmd;

    public ResourceAdapterXmlDeploymentService(ConnectorXmlDescriptor connectorXmlDescriptor, ResourceAdapter raxml,
            Connector cmd, IronJacamar ijmd, Module module, String deploymentName, File root) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        this.raxml = raxml;
        this.cmd = cmd;
        this.ijmd = ijmd;
        this.module = module;
        this.deploymentName = deploymentName;
        this.root = root;
    }

    /** create an instance **/

    @Override
    public void start(StartContext context) throws StartException {
        log.debugf("Starting sevice %s",
                ConnectorServices.RESOURCE_ADAPTER_XML_SERVICE_PREFIX.append(this.value.getDeployment().getDeploymentName()));

        final ServiceContainer container = context.getController().getServiceContainer();
        final AS7RaXmlDeployer raDeployer = new AS7RaXmlDeployer(container, connectorXmlDescriptor.getUrl(), deploymentName, root,
                module.getClassLoader(), cmd, raxml);

        raDeployer.setConfiguration(config.getValue());

        CommonDeployment raxmlDeployment = null;
        try {
            raxmlDeployment = raDeployer.doDeploy();
        } catch (Throwable t) {
            throw new StartException("Failed to start RA deployment [" + deploymentName + "]", t);
        }

        value = new ResourceAdapterDeployment(raxmlDeployment);

        registry.getValue().registerResourceAdapterDeployment(value);
        managementRepository.getValue().getConnectors().add(value.getDeployment().getConnector());

    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping sevice %s",
                ConnectorServices.RESOURCE_ADAPTER_XML_SERVICE_PREFIX.append(this.value.getDeployment().getDeploymentName()));
        managementRepository.getValue().getConnectors().remove(value.getDeployment().getConnector());
        super.stop(context);
    }

    private class AS7RaXmlDeployer extends AbstractAS7RaDeployer {

        private final ResourceAdapter ra;

        public AS7RaXmlDeployer(ServiceContainer serviceContainer, URL url, String deploymentName, File root, ClassLoader cl,
                Connector cmd, ResourceAdapter ra) {
            super(serviceContainer, url, deploymentName, root, cl, cmd);
            this.ra = ra;
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

    }
}
