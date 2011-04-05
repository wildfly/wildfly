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

import java.net.MalformedURLException;
import java.net.URL;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.sql.Driver;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.resource.ResourceException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.DataSource;

import org.jboss.as.connector.util.Injection;
import org.jboss.jca.adapters.jdbc.BaseWrapperManagedConnectionFactory;
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
import org.jboss.jca.common.api.metadata.ra.ConfigProperty;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.common.metadata.ds.DatasourcesImpl;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.NotFoundException;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.AbstractDsDeployer;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.jca.deployers.common.DeployException;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.security.SubjectFactory;

/**
 * Base service for managing a data-source.
 * @author John Bailey
 * @author maeste
 */
public abstract class AbstractDataSourceService implements Service<DataSource> {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.deployer.dsdeployer");

    public static final ServiceName SERVICE_NAME_BASE = ServiceName.JBOSS.append("data-source");
    private final InjectedValue<TransactionIntegration> transactionIntegrationValue = new InjectedValue<TransactionIntegration>();
    private final InjectedValue<Driver> driverValue = new InjectedValue<Driver>();
    private final InjectedValue<ManagementRepository> managementRepositoryValue = new InjectedValue<ManagementRepository>();
    private final InjectedValue<SubjectFactory> subjectFactory = new InjectedValue<SubjectFactory>();

    private final String jndiName;

    private javax.sql.DataSource sqlDataSource;

    protected AS7DataSourceDeployer deployer;

    protected AbstractDataSourceService(final String jndiName) {
        this.jndiName = jndiName;
    }

    public synchronized void start(StartContext startContext) throws StartException {
        try {
            final ServiceContainer container = startContext.getController().getServiceContainer();

            CommonDeployment deploymentMD = deployer.deploy(container);
            if (deploymentMD.getCfs().length != 1) {
                throw new StartException("unable to start the ds because it generate more than one cf");
            }
            sqlDataSource = (javax.sql.DataSource) deploymentMD.getCfs()[0];
            log.debugf("Adding datasource: %s", deploymentMD.getCfJndiNames()[0]);
        } catch (Throwable t) {
            throw new StartException("Error during the deployment of " + jndiName, t);
        }
    }

    public synchronized void stop(StopContext stopContext) {

        sqlDataSource = null;
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

    public Injector<ManagementRepository> getmanagementRepositoryInjector() {
        return managementRepositoryValue;
    }

    public Injector<SubjectFactory> getSubjectFactoryInjector() {
        return subjectFactory;
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
        AccessController.doPrivileged(new SetContextLoaderAction(TransactionIntegration.class.getClassLoader()));
        try {
            return transactionIntegrationValue.getValue();
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

    protected class AS7DataSourceDeployer extends AbstractDsDeployer {

        private final org.jboss.jca.common.api.metadata.ds.DataSource dataSourceConfig;

        private final XaDataSource xaDataSourceConfig;
        private ServiceContainer serviceContainer;

        public AS7DataSourceDeployer(Logger log, XaDataSource xaDataSourceConfig) {
            super(log);
            this.xaDataSourceConfig = xaDataSourceConfig;
            this.dataSourceConfig = null;
        }

        public AS7DataSourceDeployer(Logger log, org.jboss.jca.common.api.metadata.ds.DataSource dataSourceConfig) {
            super(log);
            this.dataSourceConfig = dataSourceConfig;
            this.xaDataSourceConfig = null;
        }

        public CommonDeployment deploy(ServiceContainer serviceContainer) throws DeployException {
            try {
                if (serviceContainer == null) {
                    throw new DeployException("ServiceContainer not provided");
                }
                this.serviceContainer = serviceContainer;

                DataSources dataSources = new DatasourcesImpl(
                        dataSourceConfig != null ? Arrays.asList(dataSourceConfig) : null,
                        xaDataSourceConfig != null ? Arrays.asList(xaDataSourceConfig) : null);

                CommonDeployment c = createObjectsAndInjectValue(new URL("file://DataSourceDeployment"), jndiName,
                        "uniqueJdbcLocalId", "uniqueJdbcXAId", dataSources, AbstractDataSourceService.class.getClassLoader());
                return c;
            } catch (MalformedURLException e) {
                throw new DeployException("unable to deploy", e);
            } catch (ValidateException e) {
                throw new DeployException("unable to validate and deploy ds or xads", e);
            }

        }

        @Override
        protected ClassLoader getDeploymentClassLoader(String uniqueId) {
            return driverValue.getValue().getClass().getClassLoader();
        }

        @Override
        protected String[] bindConnectionFactory(String deployment, final String jndi, Object cf) throws Throwable {
            // don't register because it's one durin add operation
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
        protected SubjectFactory getSubjectFactory(String securityDomain) throws DeployException {
            if (securityDomain == null || securityDomain.trim().equals("")) {
                return null;
            } else {
                return subjectFactory.getValue();
            }
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
                    return driverValue.getValue().getClass().getClassLoader();
                }
            });
        }

        @Override
        public TransactionIntegration getTransactionIntegration() {
            AccessController.doPrivileged(new SetContextLoaderAction(TransactionIntegration.class.getClassLoader()));
            try {
                return transactionIntegrationValue.getValue();
            } finally {
                AccessController.doPrivileged(CLEAR_ACTION);
            }
        }

        @Override
        protected ManagedConnectionFactory createMcf(XaDataSource arg0, String arg1, ClassLoader arg2)
                throws NotFoundException, DeployException {
            final XAManagedConnectionFactory xaManagedConnectionFactory = new XAManagedConnectionFactory();
            if (dataSourceConfig.getUrlDelimiter() != null) {
                try {
                    xaManagedConnectionFactory.setURLDelimiter(dataSourceConfig.getUrlDelimiter());
                } catch (ResourceException e) {
                    throw new DeployException("failed to get url delimiter", e);
                }
            }
            setMcfProperties(xaManagedConnectionFactory);
            return xaManagedConnectionFactory;

        }

        @Override
        protected ManagedConnectionFactory createMcf(org.jboss.jca.common.api.metadata.ds.DataSource arg0, String arg1,
                ClassLoader arg2) throws NotFoundException, DeployException {
            final LocalManagedConnectionFactory managedConnectionFactory = new LocalManagedConnectionFactory();
            managedConnectionFactory.setUserTransactionJndiName("java:comp/UserTransaction");
            managedConnectionFactory.setDriverClass(dataSourceConfig.getDriverClass());

            if (dataSourceConfig.getConnectionProperties() != null) {
                managedConnectionFactory.setConnectionProperties(buildConfigPropsString(dataSourceConfig
                        .getConnectionProperties()));
            }
            if (dataSourceConfig.getConnectionUrl() != null) {
                managedConnectionFactory.setConnectionURL(dataSourceConfig.getConnectionUrl());
            }
            if (dataSourceConfig.getUrlDelimiter() != null) {
                managedConnectionFactory.setURLDelimiter(dataSourceConfig.getUrlDelimiter());
            }
            setMcfProperties(managedConnectionFactory);

            return managedConnectionFactory;
        }

        private void setMcfProperties(final BaseWrapperManagedConnectionFactory managedConnectionFactory) {

            if (dataSourceConfig.getNewConnectionSql() != null) {
                managedConnectionFactory.setNewConnectionSQL(dataSourceConfig.getNewConnectionSql());
            }
            if (dataSourceConfig.getTransactionIsolation() != null) {
                managedConnectionFactory.setTransactionIsolation(dataSourceConfig.getTransactionIsolation().name());
            }

            if (dataSourceConfig.getUrlSelectorStrategyClassName() != null) {
                managedConnectionFactory.setUrlSelectorStrategyClassName(dataSourceConfig.getUrlSelectorStrategyClassName());
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
            }

            final Statement statement = dataSourceConfig.getStatement();
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
                if (validation.isValidateOnMatch()) {
                    managedConnectionFactory.setValidateOnMatch(validation.isValidateOnMatch());
                }
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

        // Override this method to change how jndiName is build in AS7
        @Override
        protected String buildJndiName(String jndiName, Boolean javaContext) {
            return super.buildJndiName(jndiName, javaContext);
        }

    }
}
