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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.resource.spi.IllegalStateException;
import javax.resource.spi.ResourceAdapter;
import javax.transaction.TransactionManager;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.deployers.processors.ParsedRaDeploymentProcessor;
import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.services.JndiService;
import org.jboss.as.connector.subsystems.connector.ConnectorSubsystemConfiguration;
import org.jboss.as.connector.util.Injection;
import org.jboss.jca.common.api.metadata.ironjacamar.IronJacamar;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.metadata.ra.ConnectionDefinition;
import org.jboss.jca.common.api.metadata.ra.Connector;
import org.jboss.jca.common.api.metadata.ra.ResourceAdapter1516;
import org.jboss.jca.common.api.metadata.ra.ra10.ResourceAdapter10;
import org.jboss.jca.core.spi.mdr.AlreadyExistsException;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.deployers.common.AbstractResourceAdapterDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

import com.arjuna.ats.jbossatx.jta.TransactionManagerService;

/**
 * A ResourceAdapterDeploymentService.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public final class ResourceAdapterDeploymentService extends AbstractResourceAdapterDeploymentService implements
        Service<ResourceAdapterDeployment> {

    private static final Logger log = Logger.getLogger("org.jboss.as.deployment.connector");

    private final Module module;
    private final ConnectorXmlDescriptor connectorXmlDescriptor;
    private final Connector cmd;
    private final IronJacamar ijmd;

    private final InjectedValue<ConnectorSubsystemConfiguration> config = new InjectedValue<ConnectorSubsystemConfiguration>();
    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txm = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();

    public ResourceAdapterDeploymentService(final ConnectorXmlDescriptor connectorXmlDescriptor, final Connector cmd,
            final IronJacamar ijmd, final Module module) {
        this.connectorXmlDescriptor = connectorXmlDescriptor;
        this.cmd = cmd;
        this.ijmd = ijmd;
        this.module = module;
    }

    @Override
    public void start(StartContext context) throws StartException {
        final ServiceContainer container = context.getController().getServiceContainer();
        final AS7RaDeployer raDeployer = new AS7RaDeployer(container);
        raDeployer.setConfiguration(config.getValue());
        final URL url = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getUrl();
        final String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();
        final File root = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getRoot();
        CommonDeployment raDeployment = null;
        try {
            raDeployment = raDeployer.doDeploy(url, deploymentName, root, module.getClassLoader(), cmd, ijmd);
        } catch (Throwable t) {
            throw new StartException("Failed to start RA deployment [" + deploymentName + "]", t);
        }

        value = new ResourceAdapterDeployment(module.getIdentifier(), raDeployment);
        registry.getValue().registerResourceAdapterDeployment(value);

        log.debugf("Starting sevice %s",
                ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(this.value.getDeployment().getDeploymentName()));
    }

    /**
     * Stop
     */
    @Override
    public void stop(StopContext context) {
        log.debugf("Stopping sevice %s",
                ConnectorServices.RESOURCE_ADAPTER_SERVICE_PREFIX.append(this.value.getDeployment().getDeploymentName()));
        super.stop(context);
    }

    public InjectedValue<ConnectorSubsystemConfiguration> getConfig() {
        return config;
    }

    public InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxm() {
        return txm;
    }

    public Injector<TransactionManagerService> getTxmInjector() {
        return txm;
    }

    public Injector<ConnectorSubsystemConfiguration> getConfigInjector() {
        return config;
    }

    private class AS7RaDeployer extends AbstractResourceAdapterDeployer {

        private String deploymentName;
        private final ServiceContainer serviceContainer;

        public AS7RaDeployer(ServiceContainer serviceContainer) {
            // validate at class level
            super(true, ParsedRaDeploymentProcessor.log);
            this.serviceContainer = serviceContainer;
        }

        public CommonDeployment doDeploy(URL url, String deploymentName, File root, ClassLoader cl, Connector cmd,
                IronJacamar ijmd) throws Throwable {

            this.setConfiguration(getConfig().getValue());

            this.deploymentName = deploymentName;

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
            JndiStrategy js = jndiStrategy.getValue();

            String[] result = js.bindConnectionFactories(deployment, new Object[] { cf }, new String[] { jndi });

            mdr.getValue().registerJndiMapping(url.toExternalForm(), cf.getClass().getName(), jndi);

            log.infof("Bound connection factory at %s", jndi);

            final JndiService jndiService = new JndiService(cf, jndi);
            ServiceBuilder<?> serviceBuilder = serviceContainer.addService(JndiService.SERVICE_NAME_BASE.append(jndi),
                    jndiService).setInitialMode(ServiceController.Mode.ACTIVE);
            serviceBuilder.install();

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

            final JndiService jndiService = new JndiService(ao, jndi);
            ServiceBuilder<?> serviceBuilder = serviceContainer.addService(JndiService.SERVICE_NAME_BASE.append(jndi),
                    jndiService).setInitialMode(ServiceController.Mode.ACTIVE);
            serviceBuilder.install();

            return result;
        }

        @Override
        protected boolean checkActivation(Connector cmd, IronJacamar ijmd) {
            if (cmd != null && ijmd != null) {
                Set<String> raClasses = new HashSet<String>();
                Set<String> ijClasses = new HashSet<String>();

                if (cmd.getVersion() == Connector.Version.V_10) {
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
                        String clz = def.getClassName();

                        if (clz == null && raClasses.size() == 1)
                            return true;

                        if (clz != null)
                            ijClasses.add(clz);
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

        @Override
        protected String registerResourceAdapterToResourceAdapterRepository(ResourceAdapter instance) {
            return raRepository.getValue().registerResourceAdapter(instance);

        }

        @Override
        protected SubjectFactory getSubjectFactory(String securityDomain) throws DeployException {
            /* TODO: We need security context service to implement it! */
            throw new DeployException("TODO: We need security context service to implement it!");
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

}
