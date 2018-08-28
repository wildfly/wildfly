/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.connector.subsystems.datasources;

import static java.lang.Thread.currentThread;
import static java.security.AccessController.doPrivileged;
import static org.jboss.as.connector.logging.ConnectorLogger.DS_DEPLOYER_LOGGER;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;

import javax.naming.Reference;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import javax.sql.DataSource;
import javax.sql.XADataSource;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.Credential;
import org.jboss.as.connector.security.ElytronSubjectFactory;
import org.jboss.as.connector.services.driver.InstalledDriver;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.Injection;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnectionFactory;
import org.jboss.jca.adapters.jdbc.JDBCResourceAdapter;
import org.jboss.jca.adapters.jdbc.local.LocalManagedConnectionFactory;
import org.jboss.jca.adapters.jdbc.spi.ClassLoaderPlugin;
import org.jboss.jca.adapters.jdbc.xa.XAManagedConnectionFactory;
import org.jboss.jca.common.api.metadata.common.Extension;
import org.jboss.jca.common.api.metadata.ds.CommonDataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.api.metadata.spec.ConfigProperty;
import org.jboss.jca.common.api.metadata.spec.XsdString;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ds.DatasourcesImpl;
import org.jboss.jca.common.metadata.ds.DriverImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.bootstrapcontext.BootstrapContextCoordinator;
import org.jboss.jca.core.connectionmanager.ConnectionManager;
import org.jboss.jca.core.security.picketbox.PicketBoxSubjectFactory;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.mdr.NotFoundException;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.DeployersLogger;
import org.jboss.jca.deployers.common.AbstractDsDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.credential.source.CredentialSource;
import org.wildfly.security.manager.WildFlySecurityManager;
import org.wildfly.security.manager.action.ClearContextClassLoaderAction;
import org.wildfly.security.manager.action.GetClassLoaderAction;
import org.wildfly.security.manager.action.SetContextClassLoaderFromClassAction;

/**
 * Base service for managing a data-source.
 * @author John Bailey
 * @author maeste
 */
public abstract class AbstractDataSourceService implements Service<DataSource> {

    /**
     * Consumers outside of the data-source subsystem should use the capability {@code org.wildfly.data-source} where
     * the dynamic name is the resource name in the model.
     */
    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("data-source");

    public static ServiceName getServiceName(ContextNames.BindInfo bindInfo) {
        return SERVICE_NAME_BASE.append(bindInfo.getBinderServiceName().getCanonicalName());
    }

    private static final DeployersLogger DEPLOYERS_LOGGER = Logger.getMessageLogger(DeployersLogger.class, AS7DataSourceDeployer.class.getName());
    protected final InjectedValue<TransactionIntegration> transactionIntegrationValue = new InjectedValue<TransactionIntegration>();
    private final InjectedValue<Driver> driverValue = new InjectedValue<Driver>();
    private final InjectedValue<ManagementRepository> managementRepositoryValue = new InjectedValue<ManagementRepository>();
    private final InjectedValue<SubjectFactory> subjectFactory = new InjectedValue<SubjectFactory>();
    private final InjectedValue<DriverRegistry> driverRegistry = new InjectedValue<DriverRegistry>();
    private final InjectedValue<CachedConnectionManager> ccmValue = new InjectedValue<CachedConnectionManager>();
    private final InjectedValue<ExecutorService> executor = new InjectedValue<ExecutorService>();
    private final InjectedValue<MetadataRepository> mdr = new InjectedValue<MetadataRepository>();
    private final InjectedValue<ServerSecurityManager> secManager = new InjectedValue<ServerSecurityManager>();
    private final InjectedValue<ResourceAdapterRepository> raRepository = new InjectedValue<ResourceAdapterRepository>();
    private final InjectedValue<AuthenticationContext> authenticationContext = new InjectedValue<>();
    private final InjectedValue<AuthenticationContext> recoveryAuthenticationContext = new InjectedValue<>();
    private final InjectedValue<ExceptionSupplier<CredentialSource, Exception>> credentialSourceSupplier = new InjectedValue<>();
    private final InjectedValue<ExceptionSupplier<CredentialSource, Exception>> recoveryCredentialSourceSupplier = new InjectedValue<>();


    private final String dsName;
    private final ContextNames.BindInfo jndiName;

    protected CommonDeployment deploymentMD;
    private WildFlyDataSource sqlDataSource;

    /**
     * The class loader to use. If null the Driver class loader will be used instead.
     */
    private final ClassLoader classLoader;

    protected AbstractDataSourceService(final String dsName, final ContextNames.BindInfo jndiName, final ClassLoader classLoader ) {
        this.dsName = dsName;
        this.classLoader = classLoader;
        this.jndiName = jndiName;
    }

    public synchronized void start(StartContext startContext) throws StartException {
        try {
            final ServiceContainer container = startContext.getController().getServiceContainer();

            deploymentMD = getDeployer().deploy(container);
            if (deploymentMD.getCfs().length != 1) {
                throw ConnectorLogger.ROOT_LOGGER.cannotStartDs();
            }
            sqlDataSource = new WildFlyDataSource((javax.sql.DataSource) deploymentMD.getCfs()[0], jndiName.getAbsoluteJndiName());
            DS_DEPLOYER_LOGGER.debugf("Adding datasource: %s", deploymentMD.getCfJndiNames()[0]);
            CommonDeploymentService cdService = new CommonDeploymentService(deploymentMD);
            final ServiceName cdServiceName = CommonDeploymentService.getServiceName(jndiName);
            startContext.getChildTarget().addService(cdServiceName, cdService)
                    // The dependency added must be the JNDI name which for subsystem resources is an alias. This service
                    // is also used in deployments where the capability service name is not registered for the service.
                    .addDependency(getServiceName(jndiName))
                    .setInitialMode(ServiceController.Mode.ACTIVE).install();
        } catch (Throwable t) {
            throw ConnectorLogger.ROOT_LOGGER.deploymentError(t, dsName);
        }
    }

    protected abstract AS7DataSourceDeployer getDeployer() throws ValidateException ;

    public void stop(final StopContext stopContext) {
        ExecutorService executorService = executor.getValue();
        Runnable r = new Runnable() {
            @Override
            public void run() {
                try {
                    stopService();
                } finally {
                    stopContext.complete();
                }
            }
        };
        try {
            executorService.execute(r);
        } catch (RejectedExecutionException e) {
            r.run();
        } finally {
            stopContext.asynchronous();
        }
    }

    /**
     * Performs the actual work of stopping the service. Should be called by {@link #stop(org.jboss.msc.service.StopContext)}
     * asynchronously from the MSC thread that invoked stop.
     */
    protected synchronized void stopService() {
        if (deploymentMD != null) {

            if (deploymentMD.getResourceAdapterKey() != null) {
                try {
                    raRepository.getValue().unregisterResourceAdapter(deploymentMD.getResourceAdapterKey());
                } catch (org.jboss.jca.core.spi.rar.NotFoundException nfe) {
                    ConnectorLogger.ROOT_LOGGER.exceptionDuringUnregistering(nfe);
                }
            }

            if (deploymentMD.getResourceAdapter() != null) {
                deploymentMD.getResourceAdapter().stop();

                if (BootstrapContextCoordinator.getInstance() != null && deploymentMD.getBootstrapContextIdentifier() != null) {
                    BootstrapContextCoordinator.getInstance().removeBootstrapContext(deploymentMD.getBootstrapContextIdentifier());
                }
            }

            if (deploymentMD.getDataSources() != null && managementRepositoryValue.getValue() != null) {
                for (org.jboss.jca.core.api.management.DataSource mgtDs : deploymentMD.getDataSources()) {
                    managementRepositoryValue.getValue().getDataSources().remove(mgtDs);
                }
            }

            if (deploymentMD.getConnectionManagers() != null) {
                for (ConnectionManager cm : deploymentMD.getConnectionManagers()) {
                    cm.shutdown();
                }
            }
        }

        sqlDataSource = null;

    }

    public CommonDeployment getDeploymentMD() {
        return deploymentMD;
    }

    public synchronized DataSource getValue() throws IllegalStateException, IllegalArgumentException {
        return sqlDataSource;
    }

    public Injector<TransactionIntegration> getTransactionIntegrationInjector() {
        return transactionIntegrationValue;
    }

    public Injector<Driver> getDriverInjector() {
        return driverValue;
    }

    public Injector<ManagementRepository> getManagementRepositoryInjector() {
        return managementRepositoryValue;
    }

    public Injector<DriverRegistry> getDriverRegistryInjector() {
        return driverRegistry;
    }

    public Injector<SubjectFactory> getSubjectFactoryInjector() {
        return subjectFactory;
    }

    public Injector<CachedConnectionManager> getCcmInjector() {
        return ccmValue;
    }

    public Injector<ExecutorService> getExecutorServiceInjector() {
        return executor;
    }

    public Injector<MetadataRepository> getMdrInjector() {
            return mdr;
        }

    public Injector<ResourceAdapterRepository> getRaRepositoryInjector() {
            return raRepository;
        }

    public Injector<ServerSecurityManager> getServerSecurityManager() {
        return secManager;
    }

    Injector<AuthenticationContext> getAuthenticationContext() {
        return authenticationContext;
    }

    Injector<AuthenticationContext> getRecoveryAuthenticationContext() {
        return recoveryAuthenticationContext;
    }

    public InjectedValue<ExceptionSupplier<CredentialSource, Exception>> getCredentialSourceSupplierInjector() {
        return credentialSourceSupplier;
    }

    public InjectedValue<ExceptionSupplier<CredentialSource, Exception>> getRecoveryCredentialSourceSupplierInjector() {
        return recoveryCredentialSourceSupplier;
    }


    protected String buildConfigPropsString(Map<String, String> configProps) {
        final StringBuffer valueBuf = new StringBuffer();
        for (Map.Entry<String, String> connProperty : configProps.entrySet()) {
            valueBuf.append(connProperty.getKey());
            valueBuf.append("=");
            valueBuf.append(connProperty.getValue());
            valueBuf.append(";");
        }
        return valueBuf.toString();
    }

    protected TransactionIntegration getTransactionIntegration() {
        if (! WildFlySecurityManager.isChecking()) {
            currentThread().setContextClassLoader(TransactionIntegration.class.getClassLoader());
        } else {
            doPrivileged(new SetContextClassLoaderFromClassAction(TransactionIntegration.class));
        }
        try {
            return transactionIntegrationValue.getValue();
        } finally {
            doPrivileged(ClearContextClassLoaderAction.getInstance());
        }
    }

    private ClassLoader driverClassLoader() {
        if(classLoader != null) {
            return classLoader;
        }
        final Class<? extends Driver> clazz = driverValue.getValue().getClass();
        return ! WildFlySecurityManager.isChecking() ? clazz.getClassLoader() : doPrivileged(new GetClassLoaderAction(clazz));
    }

    protected class AS7DataSourceDeployer extends AbstractDsDeployer {

        private final org.jboss.jca.common.api.metadata.ds.DataSource dataSourceConfig;
        private final XaDataSource xaDataSourceConfig;

        public AS7DataSourceDeployer(XaDataSource xaDataSourceConfig) {
            super();
            this.xaDataSourceConfig = xaDataSourceConfig;
            this.dataSourceConfig = null;

        }

        public AS7DataSourceDeployer(org.jboss.jca.common.api.metadata.ds.DataSource dataSourceConfig) {
            super();
            this.dataSourceConfig = dataSourceConfig;
            this.xaDataSourceConfig = null;

        }

        public CommonDeployment deploy(ServiceContainer serviceContainer) throws DeployException {
            try {
                if (serviceContainer == null) {
                    throw new DeployException(ConnectorLogger.ROOT_LOGGER.nullVar("ServiceContainer"));
                }

                HashMap<String, org.jboss.jca.common.api.metadata.ds.Driver> drivers = new HashMap<String, org.jboss.jca.common.api.metadata.ds.Driver>(
                        1);

                DataSources dataSources = null;
                if (dataSourceConfig != null) {
                    String dsClsName = dataSourceConfig.getDataSourceClass();
                    if (dsClsName != null) {
                        try {
                            Class<? extends DataSource> dsCls = driverClassLoader().loadClass(dsClsName).asSubclass(DataSource.class);
                            JdbcDriverAdd.checkDSCls(dsCls, DataSource.class);
                        } catch (OperationFailedException e) {
                            throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(e);
                        } catch (ClassCastException e) {
                            throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(ConnectorLogger.ROOT_LOGGER.notAValidDataSourceClass(dsClsName, DataSource.class.getName()));
                        } catch (ClassNotFoundException e) {
                            throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(ConnectorLogger.ROOT_LOGGER.failedToLoadDataSourceClass(dsClsName, e));
                        }
                    }

                    String driverName = dataSourceConfig.getDriver();
                    InstalledDriver installedDriver = driverRegistry.getValue().getInstalledDriver(driverName);
                    if (installedDriver != null) {
                        String moduleName = installedDriver.getModuleName() != null ? installedDriver.getModuleName().getName()
                                : null;
                        org.jboss.jca.common.api.metadata.ds.Driver driver = new DriverImpl(installedDriver.getDriverName(),
                                installedDriver.getMajorVersion(), installedDriver.getMinorVersion(),
                                moduleName, installedDriver.getDriverClassName(),
                                installedDriver.getDataSourceClassName(), installedDriver.getXaDataSourceClassName());
                        drivers.put(driverName, driver);
                    }
                    dataSources = new DatasourcesImpl(Arrays.asList(dataSourceConfig), null, drivers);
                } else if (xaDataSourceConfig != null) {
                    String xaDSClsName = xaDataSourceConfig.getXaDataSourceClass();
                    if (xaDSClsName != null) {
                        try {
                            Class<? extends XADataSource> xaDsCls = driverClassLoader().loadClass(xaDSClsName).asSubclass(XADataSource.class);
                            JdbcDriverAdd.checkDSCls(xaDsCls, XADataSource.class);
                        } catch (OperationFailedException e) {
                            throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(e);
                        } catch (ClassCastException e) {
                            throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(ConnectorLogger.ROOT_LOGGER.notAValidDataSourceClass(xaDSClsName, XADataSource.class.getName()));
                        } catch (ClassNotFoundException e) {
                            throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(ConnectorLogger.ROOT_LOGGER.failedToLoadDataSourceClass(xaDSClsName, e));
                        }
                    }

                    String driverName = xaDataSourceConfig.getDriver();
                    InstalledDriver installedDriver = driverRegistry.getValue().getInstalledDriver(driverName);
                    if (installedDriver != null) {
                        String moduleName = installedDriver.getModuleName() != null ? installedDriver.getModuleName().getName()
                                : null;
                        org.jboss.jca.common.api.metadata.ds.Driver driver = new DriverImpl(installedDriver.getDriverName(),
                                installedDriver.getMajorVersion(), installedDriver.getMinorVersion(), moduleName,
                                installedDriver.getDriverClassName(),
                                installedDriver.getDataSourceClassName(), installedDriver.getXaDataSourceClassName());
                        drivers.put(driverName, driver);
                    }
                    dataSources = new DatasourcesImpl(null, Arrays.asList(xaDataSourceConfig), drivers);
                }

                CommonDeployment c = createObjectsAndInjectValue(new URL("file://DataSourceDeployment"), dsName,
                        "uniqueJdbcLocalId", "uniqueJdbcXAId", dataSources, AbstractDataSourceService.class.getClassLoader());
                return c;
            } catch (MalformedURLException e) {
                throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(e);
            } catch (ValidateException e) {
                throw ConnectorLogger.ROOT_LOGGER.cannotDeployAndValidate(e);
            }

        }

        @Override
        protected ClassLoader getDeploymentClassLoader(String uniqueId) {
            return driverClassLoader();
        }

        @Override
        protected String[] bindConnectionFactory(String deployment, final String jndi, Object cf) throws Throwable {
            // AS7-2222: Just hack it
            if (cf instanceof javax.resource.Referenceable) {
                ((javax.resource.Referenceable)cf).setReference(new Reference(jndi));
            }

            // don't register because it's one during add operation
            return new String[] { jndi };
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

                            if (XsdString.isNull(cpmd.getConfigPropertyType())) {
                                injector.inject(o,
                                        cpmd.getConfigPropertyName().getValue(),
                                        cpmd.getConfigPropertyValue().getValue());
                            } else {
                                injector.inject(o,
                                        cpmd.getConfigPropertyName().getValue(),
                                        cpmd.getConfigPropertyValue().getValue(),
                                        cpmd.getConfigPropertyType().getValue());
                            }

                        }
                    }
                }

                return o;
            } catch (Throwable t) {
                throw ConnectorLogger.ROOT_LOGGER.deploymentFailed(t, className);
            }
        }

        @Override
        protected org.jboss.jca.core.spi.security.SubjectFactory getSubjectFactory(
                org.jboss.jca.common.api.metadata.common.Credential credential, final String jndiName) throws DeployException {
            if (credential == null)
                return null;
            // safe assertion because all parsers create Credential
            assert credential instanceof Credential;
            final String securityDomain = credential.getSecurityDomain();
            if (((Credential) credential).isElytronEnabled()) {
                try {
                    return new ElytronSubjectFactory(authenticationContext.getOptionalValue(), new java.net.URI(jndiName));
                } catch (URISyntaxException e) {
                    throw ConnectorLogger.ROOT_LOGGER.cannotDeploy(e);
                }
            } else if (securityDomain == null || securityDomain.trim().equals("") || subjectFactory.getOptionalValue() == null) {
                return null;
            } else {
                return new PicketBoxSubjectFactory(subjectFactory.getValue()){
                    @Override
                    public Subject createSubject(final String sd) {
                        ServerSecurityManager sm = secManager.getOptionalValue();
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
        }

        @Override
        public CachedConnectionManager getCachedConnectionManager() {
            return ccmValue.getOptionalValue();
        }

        @Override
        public ManagementRepository getManagementRepository() {
            return managementRepositoryValue.getValue();
        }

        @Override
        protected void initAndInjectClassLoaderPlugin(ManagedConnectionFactory mcf, CommonDataSource dsMetadata)
                throws DeployException {
            ((BaseWrapperManagedConnectionFactory) mcf).setClassLoaderPlugin(new ClassLoaderPlugin() {

                @Override
                public ClassLoader getClassLoader() {
                    return driverClassLoader();
                }
            });
        }

        @Override
        public TransactionIntegration getTransactionIntegration() {
            if (! WildFlySecurityManager.isChecking()) {
                currentThread().setContextClassLoader(TransactionIntegration.class.getClassLoader());
            } else {
                doPrivileged(new SetContextClassLoaderFromClassAction(TransactionIntegration.class));
            }
            try {
                return transactionIntegrationValue.getValue();
            } finally {
                if (! WildFlySecurityManager.isChecking()) {
                    currentThread().setContextClassLoader(null);
                } else {
                    doPrivileged(ClearContextClassLoaderAction.getInstance());
                }
            }
        }

        @Override
        protected ManagedConnectionFactory createMcf(XaDataSource arg0, String arg1, ClassLoader arg2)
                throws NotFoundException, DeployException {
            final XAManagedConnectionFactory xaManagedConnectionFactory = new XAManagedConnectionFactory(xaDataSourceConfig.getXaDataSourceProperty());

            if (xaDataSourceConfig.getUrlDelimiter() != null) {
                xaManagedConnectionFactory.setURLDelimiter(xaDataSourceConfig.getUrlDelimiter());
            }
            if (xaDataSourceConfig.getXaDataSourceClass() != null) {
                xaManagedConnectionFactory.setXADataSourceClass(xaDataSourceConfig.getXaDataSourceClass());
            }
            if (xaDataSourceConfig.getUrlSelectorStrategyClassName() != null) {
                xaManagedConnectionFactory
                        .setUrlSelectorStrategyClassName(xaDataSourceConfig.getUrlSelectorStrategyClassName());
            }
            if (xaDataSourceConfig.getXaPool() != null && xaDataSourceConfig.getXaPool().isSameRmOverride() != null) {
                xaManagedConnectionFactory.setIsSameRMOverrideValue(xaDataSourceConfig.getXaPool().isSameRmOverride());
            }

            if (xaDataSourceConfig.getNewConnectionSql() != null) {
                xaManagedConnectionFactory.setNewConnectionSQL(xaDataSourceConfig.getNewConnectionSql());
            }

            if (xaDataSourceConfig.getUrlSelectorStrategyClassName() != null) {
                xaManagedConnectionFactory
                        .setUrlSelectorStrategyClassName(xaDataSourceConfig.getUrlSelectorStrategyClassName());
            }

            setMcfProperties(xaManagedConnectionFactory, xaDataSourceConfig, xaDataSourceConfig.getStatement());
            return xaManagedConnectionFactory;

        }

        @Override
        protected ManagedConnectionFactory createMcf(org.jboss.jca.common.api.metadata.ds.DataSource arg0, String arg1,
                ClassLoader arg2) throws NotFoundException, DeployException {
            final LocalManagedConnectionFactory managedConnectionFactory = new LocalManagedConnectionFactory();
            managedConnectionFactory.setDriverClass(dataSourceConfig.getDriverClass());

            if (dataSourceConfig.getUrlDelimiter() != null) {
                try {
                    managedConnectionFactory.setURLDelimiter(dataSourceConfig.getUrlDelimiter());
                } catch (Exception e) {
                    throw ConnectorLogger.ROOT_LOGGER.failedToGetUrlDelimiter(e);
                }
            }

            if (dataSourceConfig.getDataSourceClass() != null) {
                managedConnectionFactory.setDataSourceClass(dataSourceConfig.getDataSourceClass());
            }

            if (dataSourceConfig.getConnectionProperties() != null) {
                managedConnectionFactory.setConnectionProperties(buildConfigPropsString(dataSourceConfig
                        .getConnectionProperties()));
            }
            if (dataSourceConfig.getConnectionUrl() != null) {
                managedConnectionFactory.setConnectionURL(dataSourceConfig.getConnectionUrl());
            }

            if (dataSourceConfig.getNewConnectionSql() != null) {
                managedConnectionFactory.setNewConnectionSQL(dataSourceConfig.getNewConnectionSql());
            }

            if (dataSourceConfig.getUrlSelectorStrategyClassName() != null) {
                managedConnectionFactory.setUrlSelectorStrategyClassName(dataSourceConfig.getUrlSelectorStrategyClassName());
            }
            setMcfProperties(managedConnectionFactory, dataSourceConfig, dataSourceConfig.getStatement());

            return managedConnectionFactory;
        }

        private void setMcfProperties(final BaseWrapperManagedConnectionFactory managedConnectionFactory,
                CommonDataSource dataSourceConfig, final Statement statement) {

            if (dataSourceConfig.getTransactionIsolation() != null) {
                managedConnectionFactory.setTransactionIsolation(dataSourceConfig.getTransactionIsolation().name());
            }

            final DsSecurity security = dataSourceConfig.getSecurity();
            if (security != null) {
                if (security.getUserName() != null) {
                    managedConnectionFactory.setUserName(security.getUserName());
                }
                if (security.getPassword() != null) {
                    managedConnectionFactory.setPassword(security.getPassword());
                }
            }

            final TimeOut timeOut = dataSourceConfig.getTimeOut();
            if (timeOut != null) {
                if (timeOut.getUseTryLock() != null) {
                    managedConnectionFactory.setUseTryLock(timeOut.getUseTryLock().intValue());
                }
                if (timeOut.getQueryTimeout() != null) {
                    managedConnectionFactory.setQueryTimeout(timeOut.getQueryTimeout().intValue());
                }
                if (timeOut.isSetTxQueryTimeout()) {
                    managedConnectionFactory.setTransactionQueryTimeout(true);
                }
            }

            if (statement != null) {
                if (statement.getTrackStatements() != null) {
                    managedConnectionFactory.setTrackStatements(statement.getTrackStatements().name());
                }
                if (statement.isSharePreparedStatements() != null) {
                    managedConnectionFactory.setSharePreparedStatements(statement.isSharePreparedStatements());
                }
                if (statement.getPreparedStatementsCacheSize() != null) {
                    managedConnectionFactory.setPreparedStatementCacheSize(statement.getPreparedStatementsCacheSize()
                            .intValue());
                }
            }

            final Validation validation = dataSourceConfig.getValidation();
            if (validation != null) {
                if (validation.getCheckValidConnectionSql() != null) {
                    managedConnectionFactory.setCheckValidConnectionSQL(validation.getCheckValidConnectionSql());
                }
                final Extension validConnectionChecker = validation.getValidConnectionChecker();
                if (validConnectionChecker != null) {
                    if (validConnectionChecker.getClassName() != null) {
                        managedConnectionFactory.setValidConnectionCheckerClassName(validConnectionChecker.getClassName());
                    }
                    if (validConnectionChecker.getConfigPropertiesMap() != null) {
                        managedConnectionFactory
                                .setValidConnectionCheckerProperties(buildConfigPropsString(validConnectionChecker
                                        .getConfigPropertiesMap()));
                    }
                }
                final Extension exceptionSorter = validation.getExceptionSorter();
                if (exceptionSorter != null) {
                    if (exceptionSorter.getClassName() != null) {
                        managedConnectionFactory.setExceptionSorterClassName(exceptionSorter.getClassName());
                    }
                    if (exceptionSorter.getConfigPropertiesMap() != null) {
                        managedConnectionFactory.setExceptionSorterProperties(buildConfigPropsString(exceptionSorter
                                .getConfigPropertiesMap()));
                    }
                }
                final Extension staleConnectionChecker = validation.getStaleConnectionChecker();
                if (staleConnectionChecker != null) {
                    if (staleConnectionChecker.getClassName() != null) {
                        managedConnectionFactory.setStaleConnectionCheckerClassName(staleConnectionChecker.getClassName());
                    }
                    if (staleConnectionChecker.getConfigPropertiesMap() != null) {
                        managedConnectionFactory
                                .setStaleConnectionCheckerProperties(buildConfigPropsString(staleConnectionChecker
                                        .getConfigPropertiesMap()));
                    }
                }
            }
        }

        // Override this method to change how dsName is build in AS7
        @Override
        protected String buildJndiName(String rawJndiName, Boolean javaContext) {
            final String jndiName;
            if (!rawJndiName.startsWith("java:") && javaContext) {
                if (rawJndiName.startsWith("jboss/")) {
                    // Bind to java:jboss/ namespace
                    jndiName = "java:" + rawJndiName;
                } else {
                    // Bind to java:/ namespace
                    jndiName= "java:/" + rawJndiName;
                }
            } else {
                jndiName = rawJndiName;
            }
            return jndiName;
        }

        @Override
        protected DeployersLogger getLogger() {
            return DEPLOYERS_LOGGER;
        }

        @Override
        protected javax.resource.spi.ResourceAdapter createRa(String uniqueId, ClassLoader cl) throws NotFoundException, DeployException {

            List<? extends ConfigProperty> l = new ArrayList<ConfigProperty>();

            javax.resource.spi.ResourceAdapter rar =
                    (javax.resource.spi.ResourceAdapter) initAndInject(JDBCResourceAdapter.class.getName(), l, cl);

            return rar;
        }

        @Override
        protected String registerResourceAdapterToResourceAdapterRepository(javax.resource.spi.ResourceAdapter instance) {
            return raRepository.getValue().registerResourceAdapter(instance);
        }

    }


}
