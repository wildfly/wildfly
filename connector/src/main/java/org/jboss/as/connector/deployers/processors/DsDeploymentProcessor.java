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

import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

import javax.transaction.TransactionManager;

import org.jboss.as.connector.metadata.xmldescriptors.ConnectorXmlDescriptor;
import org.jboss.as.connector.registry.ResourceAdapterDeploymentRegistry;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.deployment.DeploymentPhases;
import org.jboss.as.deployment.module.ModuleConfig;
import org.jboss.as.deployment.module.ModuleDependencies;
import org.jboss.as.deployment.module.ModuleDeploymentProcessor;
import org.jboss.as.deployment.unit.DeploymentUnitContext;
import org.jboss.as.deployment.unit.DeploymentUnitProcessingException;
import org.jboss.as.deployment.unit.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.naming.JndiStrategy;
import org.jboss.jca.deployers.common.AbstractDsDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.InjectedValue;
import org.jboss.msc.value.Value;

/**
 * DeploymentUnitProcessor responsible for using IronJacamar metadata and create
 * service for ResourceAdapter.
 * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsDeploymentProcessor implements DeploymentUnitProcessor {
    public static final long PRIORITY = DeploymentPhases.INSTALL_SERVICES.plus(120L);
    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    private final InjectedValue<MetadataRepository> mdr = new InjectedValue<MetadataRepository>();

    private final InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService> txm = new InjectedValue<com.arjuna.ats.jbossatx.jta.TransactionManagerService>();

    private final InjectedValue<DataSources> dsValue = new InjectedValue<DataSources>();

    private final InjectedValue<ResourceAdapterDeploymentRegistry> registry = new InjectedValue<ResourceAdapterDeploymentRegistry>();

    private final InjectedValue<JndiStrategy> jndiStrategy = new InjectedValue<JndiStrategy>();

    public DsDeploymentProcessor() {
        super();
    }

    /**
     * Deploy datasources
     * @param context the deployment unit context
     * @throws DeploymentUnitProcessingException
     */
    public void processDeployment(DeploymentUnitContext context) throws DeploymentUnitProcessingException {
        final ConnectorXmlDescriptor connectorXmlDescriptor = context.getAttachment(ConnectorXmlDescriptor.ATTACHMENT_KEY);

        Module module = context.getAttachment(ModuleDeploymentProcessor.MODULE_ATTACHMENT_KEY);

        String deploymentName = connectorXmlDescriptor == null ? null : connectorXmlDescriptor.getDeploymentName();

        DataSources datasources = null;

        datasources = dsValue.getValue();

        if (datasources == null || deploymentName == null || !deploymentName.startsWith("jdbc"))
            return;

        log.tracef("Processing datasource deployement: %s", datasources);

        if (module == null)
            throw new DeploymentUnitProcessingException("Failed to get module attachment for deployment: " + context.getName());

        try {
            if (deploymentName.indexOf("local") != -1) {
                // Local datasources
                List<DataSource> dss = datasources.getDataSource();
                if (dss != null && dss.size() > 0) {
                    for (DataSource ds : dss) {
                        try {
                            log.tracef("Processing datasource deployement: %s", ds);

                            if (ds.getModule() != null && !ds.getModule().trim().equals("")) {
                                ModuleIdentifier jdbcIdentifier = ModuleIdentifier.fromString(ds.getModule());
                                Module jdbcModule = Module.getDefaultModuleLoader().loadModule(jdbcIdentifier);

                                // Hack: Link the jdbcModule
                                ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(jdbcIdentifier, true,
                                        false, false));
                            } else {
                                log.warnf("No module defined for %s", ds.getJndiName());
                            }
                        } catch (ModuleLoadException mle) {
                            log.warnf("Unable to resolve %s for %s", ds.getModule(), ds.getJndiName());
                        }
                    }

                    AS7Deployer deployer = new AS7Deployer(jndiStrategy.getValue(), module.getClassLoader(), log);

                    String uniqueJdbcLocalId = deploymentName;
                    String uniqueJdbcXAId = null;

                    deployer.setTransactionManager(getTransactionManager());
                    deployer.setMetadataRepository(mdr.getValue());

                    deployer.doDeploy(new URL("file://DataSourceDeployment"), deploymentName, uniqueJdbcLocalId,
                            uniqueJdbcXAId, datasources, module.getClassLoader());
                }
            } else {
                // XA datasources
                List<XaDataSource> xadss = datasources.getXaDataSource();
                if (xadss != null && xadss.size() > 0) {
                    for (XaDataSource xads : xadss) {
                        try {
                            log.tracef("Processing xa-datasource deployement: %s", xads);

                            if (xads.getModule() != null && !xads.getModule().trim().equals("")) {
                                ModuleIdentifier jdbcIdentifier = ModuleIdentifier.fromString(xads.getModule());
                                Module jdbcModule = Module.getDefaultModuleLoader().loadModule(jdbcIdentifier);

                                // Hack: Link the jdbcModule
                                ModuleDependencies.addDependency(context, new ModuleConfig.Dependency(jdbcIdentifier, true,
                                        false, false));
                            } else {
                                log.warnf("No module defined for %s", xads.getJndiName());
                            }
                        } catch (ModuleLoadException mle) {
                            log.warnf("Unable to resolve %s for %s", xads.getModule(), xads.getJndiName());
                        }
                    }

                    AS7Deployer deployer = new AS7Deployer(jndiStrategy.getValue(), module.getClassLoader(), log);

                    String uniqueJdbcLocalId = null;
                    String uniqueJdbcXAId = deploymentName;

                    deployer.setTransactionManager(getTransactionManager());
                    deployer.setMetadataRepository(mdr.getValue());

                    deployer.doDeploy(new URL("file://DataSourceDeployment"), deploymentName, uniqueJdbcLocalId,
                            uniqueJdbcXAId, datasources, module.getClassLoader());
                }
            }
        } catch (Throwable t) {
            throw new DeploymentUnitProcessingException(t);
        }
    }

    protected TransactionManager getTransactionManager() {
        AccessController.doPrivileged(new SetContextLoaderAction(com.arjuna.ats.jbossatx.jta.TransactionManagerService.class
                .getClassLoader()));
        try {
            return getTxm().getValue().getTransactionManager();
        } finally {
            AccessController.doPrivileged(CLEAR_ACTION);
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

    public Value<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxm() {
        return txm;
    }

    private static class AS7Deployer extends AbstractDsDeployer {

        private JndiStrategy js;
        private ClassLoader cl;

        public AS7Deployer(JndiStrategy js, ClassLoader cl, Logger log) {
            super(log);
            this.js = js;
            this.cl = cl;
        }

        public AS7Deployer(ClassLoader cl, Logger log) {
            super(log);
            this.cl = cl;
        }

        public CommonDeployment doDeploy(URL url, String deploymentName, String uniqueJdbcLocalId, String uniqueJdbcXaId,
                DataSources dataSources, ClassLoader parentClassLoader) throws DeployException {

            return createObjectsAndInjectValue(url, deploymentName, uniqueJdbcLocalId, uniqueJdbcXaId, dataSources,
                    parentClassLoader);
        }

        @Override
        protected ClassLoader getDeploymentClassLoader(String uniqueId) {
            return cl;
        }

        @Override
        protected String[] bindConnectionFactory(String deployment, String jndi, Object cf) throws Throwable {
            String[] result = js.bindConnectionFactories(deployment, new Object[] { cf }, new String[] { jndi });
            log.infof("Bound Data Source at %s", jndi);
            return result;
        }

        @Override
        protected Object initAndInject(String className, List<? extends ConfigProperty> configs, ClassLoader cl)
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

    }

    public Injector<MetadataRepository> getMdrInjector() {
        return mdr;
    }

    public Injector<DataSources> getDsValueInjector() {
        return dsValue;
    }

    public Injector<ResourceAdapterDeploymentRegistry> getRegistryInjector() {
        return registry;
    }

    public Injector<com.arjuna.ats.jbossatx.jta.TransactionManagerService> getTxmInjector() {
        return txm;
    }

    public Injector<JndiStrategy> getJndiInjector() {
        return jndiStrategy;
    }
}
