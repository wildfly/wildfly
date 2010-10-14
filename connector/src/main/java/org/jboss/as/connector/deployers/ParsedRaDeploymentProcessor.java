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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.connector.ResourceAdapterDeploymentService;
import org.jboss.as.connector.annotations.repository.jandex.JandexAnnotationRepositoryImpl;
import org.jboss.as.connector.descriptor.ConnectorXmlDescriptor;
import org.jboss.as.connector.descriptor.IronJacamarXmlDescriptor;
import org.jboss.as.connector.mdr.MdrServices;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.processor.AnnotationIndexProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jca.common.annotations.Annotations;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.ra.Connector.Version;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.api.metadata.ra.ra10.ResourceAdapter10;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.common.spi.annotations.repository.AnnotationRepository;
import org.jboss.jca.core.naming.ExplicitJndiStrategy;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.deployers.common.AbstractResourceAdapterDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 * @author <a href="stefano.maestri@jboss.com">Stefano Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class ParsedRaDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(101L);
    public static final Logger log = Logger.getLogger("org.jboss.as.deployment.service");

    private final Value<MetadataRepository> mdr;
    private final Value<TransactionManager> txm;
    private final Value<ConnectorSubsystemConfiguration> config;

    public ParsedRaDeploymentProcessor(Value<MetadataRepository> mdr, Value<TransactionManager> txm,
            Value<ConnectorSubsystemConfiguration> config) {
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

            raDeployer.setConfiguration(config.getValue());

            CommonDeployment raDeployment = raDeployer.doDeploy(connectorXmlDescriptor.getUrl(),
                    connectorXmlDescriptor.getDeploymentName(), connectorXmlDescriptor.getRoot(), classLoader, cmd, ijmd);

            final BatchBuilder batchBuilder = context.getBatchBuilder();
            final InjectedValue<MetadataRepository> mdrInjector = new InjectedValue<MetadataRepository>();

            batchBuilder
                    .addService(
                            ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX
                                    .append(connectorXmlDescriptor.getDeploymentName()),
                            new ResourceAdapterDeploymentService(raDeployment, mdr))
                    .addDependency(MdrServices.IRONJACAMAR_MDR, MetadataRepository.class, mdrInjector)
                    .setInitialMode(Mode.ON_DEMAND);
            batchBuilder.install();
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
            super(true, ParsedRaDeploymentProcessor.log);
        }

        public CommonDeployment doDeploy(URL url, String deploymentName, File root, ClassLoader cl, Connector cmd,
                IronJacamar ijmd) throws Throwable {
            // TODO
            this.setConfiguration(getConfig().getValue());

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, cl, cmd, ijmd);

            return dep;
        }

        @Override
        public String[] bindConnectionFactory(URL url, String deployment, Object cf) throws Throwable {
            throw new IllegalStateException("Non-explicit JNDI bindings not supported");
        }

        @Override
        public String[] bindConnectionFactory(URL url, String deployment, Object cf, String jndi) throws Throwable {
            JndiStrategy js = new ExplicitJndiStrategy();

            String[] result = js.bindConnectionFactories(deployment, new Object[] { cf }, new String[] { jndi });

            mdr.getValue().registerJndiMapping(url.toExternalForm(), cf.getClass().getName(), jndi);

            return result;
        }

        @Override
        protected boolean checkActivation(Connector cmd, IronJacamar ijmd) {
            if (cmd != null && ijmd != null) {
                Set<String> raClasses = new HashSet<String>();
                Set<String> ijClasses = new HashSet<String>();

                if (cmd.getVersion() == Version.V_10) {
                    ResourceAdapter10 ra10 = (ResourceAdapter10) cmd.getResourceadapter();
                    raClasses.add(ra10.getManagedConnectionFactoryClass().getValue());
                } else {
                    ResourceAdapter1516 ra = (ResourceAdapter1516) cmd.getResourceadapter();
                    if (ra != null && ra.getOutboundResourceadapter() != null
                            && ra.getOutboundResourceadapter().getConnectionDefinitions() != null) {
                        List<ConnectionDefinition> cdMetas = ra.getOutboundResourceadapter().getConnectionDefinitions();
                        if (cdMetas.size() > 0) {
                            for (ConnectionDefinition cdMeta : cdMetas) {
                                raClasses.add(cdMeta.getManagedConnectionFactoryClass().getValue());
                            }
                        }
                    }
                }

                if (raClasses.size() == 0)
                    return false;

                if (ijmd.getConnectionDefinitions() != null) {
                    for (org.jboss.jca.common.api.metadata.common.CommonConnDef def : ijmd.getConnectionDefinitions()) {
                        ijClasses.add(def.getClassName());
                    }
                }

                for (String clz : raClasses) {
                    if (!ijClasses.contains(clz))
                        return false;
                }

                return true;
            }

            return false;

        }

        @Override
        protected boolean checkConfigurationIsValid() {
            return this.getConfiguration() != null;
        }

        @Override
        protected PrintWriter getLogPrintWriter() {
            return new PrintWriter(System.out);
        }

        @Override
        protected File getReportDirectory() {
            // TODO: evaluate if provide something in config about that. atm
            // returning null and so skip its use
            return null;
        }

        @Override
        protected TransactionManager getTransactionManager() {
            return getTxm().getValue();
        }

        @Override
        public Object initAndInject(String className, List<? extends ConfigProperty> configs, ClassLoader cl)
                throws DeployException {
            try {
                Class clz = Class.forName(className, true, cl);
                Object o = clz.newInstance();

                if (configs != null) {
                    Injection injector = new Injection();
                    for (ConfigProperty cpmd : configs) {
                        if (cpmd.isValueSet())
                            injector.inject(cpmd.getConfigPropertyType().getValue(), cpmd.getConfigPropertyName().getValue(),
                                    cpmd.getConfigPropertyValue().getValue(), o);
                    }
                }

                return o;
            } catch (Throwable t) {
                throw new DeployException("Deployment " + className + " failed", t);
            }
        }

        @Override
        protected void registerResourceAdapterToMDR(URL url, File file, Connector connector, IronJacamar ij)
                throws AlreadyExistsException {
            mdr.getValue().registerResourceAdapter(url.toExternalForm(), file, connector, ij);
        }
    }
}
