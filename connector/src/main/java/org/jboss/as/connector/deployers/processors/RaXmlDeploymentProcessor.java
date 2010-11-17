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

package org.jboss.as.connector.deployers.processors;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;
import java.util.Set;

import javax.transaction.TransactionManager;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterDeployment;
import org.jboss.as.connector.metadata.deployment.ResourceAdapterXmlDeploymentService;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.subsystems.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter;
import org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapters;
import org.jboss.jca.common.metadata.merge.Merger;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.deployers.common.AbstractResourceAdapterDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 * @author <a href="mailto:stefano.maestri@redhat.comdhat.com">Stefano
 *         Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class RaXmlDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(110L);
    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.raxmldeployer");

    private final InjectedValue<MetadataRepository> mdr = new InjectedValue<MetadataRepository>();
    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txm = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();
    private final InjectedValue<ConnectorSubsystemConfiguration> config = new InjectedValue<ConnectorSubsystemConfiguration>();
    private final InjectedValue<ResourceAdapters> raxmlValue = new InjectedValue<ResourceAdapters>();
    private final InjectedValue<ResourceAdapterDeploymentRegistry> registry = new InjectedValue<ResourceAdapterDeploymentRegistry>();
    private final InjectedValue<JndiStrategy> jndiStrategy = new InjectedValue<JndiStrategy>();

    public RaXmlDeploymentProcessor() {
        super();
    }

    /**
     * Process a deployment for a Connector. Will install a {@Code
     * JBossService} for this ResourceAdapter.
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = context.getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);
        if(connectorXmlDescriptor == null) {
            return;  // Skip non ra deployments
        }
        ResourceAdapters raxmls = null;

        raxmls = raxmlValue.getValue();

        if (raxmls == null)
            return;

        log.tracef("processing Raxml");
        Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);

        if (module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for deployment: " + context.getName());

        try {
            final ClassLoader classLoader = module.getClassLoader();

            final BatchBuilder batchBuilder = context.getBatchBuilder();

            for (org.jboss.jca.common.api.metadata.resourceadapter.ResourceAdapter raxml : raxmls.getResourceAdapters()) {

                String archive = raxml.getArchive();
                URL deployment = null;
                Set<String> deployments = mdr.getValue().getResourceAdapters();

                for (String s : deployments) {
                    if (s.endsWith(archive) || s.endsWith(archive.substring(0, archive.indexOf(".rar"))))
                        deployment = new URL(s);
                }

                if (deployment != null) {

                    Connector cmd = mdr.getValue().getResourceAdapter(deployment.toExternalForm());
                    IronJacamar ijmd = mdr.getValue().getIronJacamar(deployment.toExternalForm());
                    File root = mdr.getValue().getRoot(deployment.toExternalForm());

                    cmd = (new Merger()).mergeConnectorWithCommonIronJacamar(raxml, cmd);

                    String deploymentName = archive.substring(0, archive.indexOf(".rar"));

                    AS7RaDeployer raDeployer = new AS7RaDeployer();

                    raDeployer.setConfiguration(config.getValue());

                    CommonDeployment raxmlDeployment = raDeployer.doDeploy(connectorXmlDescriptor.getUrl(), deploymentName,
                            root, classLoader, cmd, ijmd, raxml);

                    ResourceAdapterDeployment dply = new ResourceAdapterDeployment(module.getIdentifier(), raxmlDeployment);
                    ResourceAdapterXmlDeploymentService service = new ResourceAdapterXmlDeploymentService(dply);
                    // Create the service
                    batchBuilder
                            .addService(ConnectorServices.RESOURCE_ADAPTER_XML_SERVICE_PREFIX.append(deploymentName), service)
                            .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, service.getMdrInjector())
                            .addDependency(ConnectorServices.RESOURCE_ADAPTER_REGISTRY_SERVICE,
                                    ResourceAdapterDeploymentRegistry.class, service.getRegistryInjector())
                            .addDependency(ConnectorServices.JNDI_STRATEGY_SERVICE,
                                    JndiStrategy.class, service.getJndiInjector())
                            .setInitialMode(Mode.ACTIVE);

                    registry.getValue().registerResourceAdapterDeployment(dply);
                }
            }
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }

    public Value<MetadataRepository> getMdr() {
        return mdr;
    }

    public Injector<ResourceAdapterDeploymentRegistry> getRegistryInjector() {
        return registry;
    }

    public Injector<JndiStrategy> getJndiInjector() {
        return jndiStrategy;
    }

    public Value<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxm() {
        return txm;
    }

    public Value<ConnectorSubsystemConfiguration> getConfig() {
        return config;
    }

    private class AS7RaDeployer extends AbstractResourceAdapterDeployer {

        private String deploymentName;

        public AS7RaDeployer() {
            // validate at class level
            super(true, ParsedRaDeploymentProcessor.log);
        }

        public CommonDeployment doDeploy(URL url, String deploymentName, File root, ClassLoader cl, Connector cmd,
                IronJacamar ijmd, ResourceAdapter ra) throws Throwable {

            this.setConfiguration(getConfig().getValue());

            this.deploymentName = deploymentName;

            this.start();

            CommonDeployment dep = this.createObjectsAndInjectValue(url, deploymentName, root, cl, cmd, ijmd, ra);

            return dep;
        }

        @Override
        public String[] bindConnectionFactory(URL url, String deployment, Object cf) throws Throwable {
            throw new IllegalStateException("Non-explicit JNDI bindings not supported");
        }

        @Override
        public String[] bindConnectionFactory(URL url, String deployment, Object cf, String jndi) throws Throwable {
            JndiStrategy js = jndiStrategy.getValue();

            String[] result = js.bindConnectionFactories(deployment, new Object[] { cf }, new String[] { jndi });

            mdr.getValue().registerJndiMapping(url.toExternalForm(), cf.getClass().getName(), jndi);

            log.infof("Bound connection factory at %s", jndi);

            return result;
        }

        @Override
        public String[] bindAdminObject(URL url, String deployment, Object ao) throws Throwable {
            throw new IllegalStateException("Non-explicit JNDI bindings not supported");
        }

        @Override
        public String[] bindAdminObject(URL url, String deployment, Object ao, String jndi) throws Throwable {
            JndiStrategy js = jndiStrategy.getValue();

            String[] result = js.bindAdminObjects(deployment, new Object[] { ao }, new String[] { jndi });

            mdr.getValue().registerJndiMapping(url.toExternalForm(), ao.getClass().getName(), jndi);

            log.infof("Bound admin object at %s", jndi);

            return result;
        }

        @Override
        protected boolean checkActivation(Connector cmd, IronJacamar ijmd) {
            return true;
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
            AccessController.doPrivileged(new SetContextLoaderAction(
                    com.arjuna.ats.jbossatx.jta.TransactionManagerService.class.getClassLoader()));
            try {
                return getTxm().getValue().getTransactionManager();
            } finally {
                AccessController.doPrivileged(CLEAR_ACTION);
            }
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
                        if (cpmd.isValueSet()) {
                            boolean setValue = true;

                            if (cpmd instanceof org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16) {
                                org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16 cpmd16 = (org.jboss.jca.common.api.metadata.ra.ra16.ConfigProperty16) cpmd;

                                if (cpmd16.getConfigPropertyIgnore() != null && cpmd16.getConfigPropertyIgnore().booleanValue())
                                    setValue = false;
                            }

                            if (setValue)
                                injector.inject(cpmd.getConfigPropertyType().getValue(), cpmd.getConfigPropertyName()
                                        .getValue(), cpmd.getConfigPropertyValue().getValue(), o);
                        }
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
            log.debugf("Registering ResourceAdapter %s", deploymentName);
            mdr.getValue().registerResourceAdapter(deploymentName, file, connector, ij);
        }
    }

    private static final SetContextLoaderAction CLEAR_ACTION = new SetContextLoaderAction(null);

    private static class SetContextLoaderAction implements PrivilegedAction<Void> {

        private final ClassLoader classLoader;

        public SetContextLoaderAction(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public Void run() {
            Thread.currentThread().setContextClassLoader(classLoader);
            return null;
        }
    }

    public Injector<ResourceAdapters> getRaxmlValueInjector() {
        return raxmlValue;
    }

    public Injector<MetadataRepository> getMdrInjector() {
        return mdr;
    }

    public Injector<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxmInjector() {
        return txm;
    }

    public Injector<ConnectorSubsystemConfiguration> getConfigInjector() {
        return config;
    }
}
