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

package org.jboss.as.connector.deployers;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.util.List;

import javax.resource.spi.ResourceAdapter;
import javax.transaction.TransactionManager;

import org.jboss.as.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.connector.annotations.repository.jandex.JandexAnnotationRepositoryImpl;
import org.jboss.as.connector.descriptor.ConnectorXmlDescriptor;
import org.jboss.as.connector.descriptor.IronJacamarXmlDescriptor;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jca.common.annotations.Annotations;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.common.spi.annotations.repository.AnnotationRepository;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.deployers.common.AbstractResourceAdapterDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.value.Value;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * serivice for ResourceAdapter.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 */
public class ParsedRaDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(101L);
    public static final Logger log = Logger.getLogger("org.jboss.as.deployment.service");

    private final Value<MetadataRepository> mdr;
    private final Value<TransactionManager> txm;
    private final Value<ConnectorSubsystemConfiguration> config;

    public ParsedRaDeploymentProcessor(Value<MetadataRepository> mdr, Value<TransactionManager> txm,
            Value<ConnectorSubsystemConfiguration> config) {
        super();
        this.mdr = mdr;
        this.txm = txm;
        this.config = config;
    }

    /**
     * Process a deployment for a Connector. Will install a {@Code
     * JBossService} for this ResourceAdapter.
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = context.getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        final IronJacamarXmlDescriptor ironJacamarXmlDescriptor = context
                .getAttachment(IronJacamarXmlDescriptor.ATTACHMENT_KEY);

        if (connectorXmlDescriptor == null)
            return;

        final Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);
        if (module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for deployment: " + context.getName());

        final ClassLoader classLoader = module.getClassLoader();

        // TODO get from mdr
        Connector cmd = connectorXmlDescriptor.getConnector();
        final IronJacamar ijmd = ironJacamarXmlDescriptor.getIronJacamar();

        try {
            // Annotation merging
            Annotations annotator = new Annotations();
            AnnotationRepository repository = new JandexAnnotationRepositoryImpl(
                    context.getAttachment(AnnotationIndexProcessor.ATTACHMENT_KEY), classLoader);
            cmd = annotator.merge(cmd, repository);

            // Validate metadata
            cmd.validate();

            // Merge metadata
            cmd = (new Merger()).mergeConnectorWithCommonIronJacamar(ijmd, cmd);

            AS7RaDeployer raDeployer = new AS7RaDeployer();

            ResourceAdapter ra = raDeployer.doDeploy(connectorXmlDescriptor.getUrl(),
                    connectorXmlDescriptor.getDeploymentName(), connectorXmlDescriptor.getRoot(), classLoader, cmd, ijmd);

            // Create the service
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }

    public Value<MetadataRepository> getMdr() {
        return mdr;
    }

    public Value<TransactionManager> getTxm() {
        return txm;
    }

    public Value<ConnectorSubsystemConfiguration> getConfig() {
        return config;
    }

    private class AS7RaDeployer extends AbstractResourceAdapterDeployer {

        public AS7RaDeployer() {
            // validate at class level
            super(true);
        }

        public ResourceAdapter doDeploy(URL url, String deploymentName, File root, ClassLoader cl, Connector cmd,
                IronJacamar ijmd) throws Throwable {
            // TODO
            this.setConfiguration(getConfig().getValue());

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, null, cl, cmd, ijmd, null);

            return dep.getResourceAdapter();

        }

        @Override
        protected String[] bindConnectionFactory(URL arg0, String arg1, Object arg2) throws Throwable {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected String[] bindConnectionFactory(URL arg0, String arg1, Object arg2, String arg3) throws Throwable {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected boolean checkActivation(Connector arg0, IronJacamar arg1) {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        protected boolean checkConfigurationIsValid() {
            // TODO Auto-generated method stub
            return false;
        }

        @Override
        protected PrintWriter getLogPrintWriter() {
            return null;
        }

        @Override
        protected File getReportDirectory() {
            return null;
        }

        @Override
        protected TransactionManager getTransactionManager() {
            return getTxm().getValue();
        }

        @Override
        protected Object initAndInject(String arg0, List<? extends ConfigProperty> arg1, ClassLoader arg2)
                throws DeployException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        protected void registerResourceAdapterToMDR(URL arg0, File arg1, Connector arg2, IronJacamar arg3)
                throws AlreadyExistsException {
            // TODO Auto-generated method stub

        }

    }
}
