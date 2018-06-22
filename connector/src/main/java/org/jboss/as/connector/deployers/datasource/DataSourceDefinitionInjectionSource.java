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

package org.jboss.as.connector.deployers.datasource;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.Iterator;
import java.util.Map;

import javax.sql.XADataSource;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.ds.DsSecurityImpl;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.connector.subsystems.datasources.DataSourceReferenceFactoryService;
import org.jboss.as.connector.subsystems.datasources.LocalDataSourceService;
import org.jboss.as.connector.subsystems.datasources.ModifiableDataSource;
import org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource;
import org.jboss.as.connector.subsystems.datasources.XaDataSourceService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.ee.component.Attachments;
import org.jboss.as.ee.component.EEModuleDescription;
import org.jboss.as.ee.resource.definition.ResourceDefinitionInjectionSource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.reflect.ClassReflectionIndexUtil;
import org.jboss.as.server.deployment.reflect.DeploymentReflectionIndex;
import org.jboss.invocation.proxy.MethodIdentifier;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.ds.TransactionIsolation;
import org.jboss.jca.common.metadata.ds.DsPoolImpl;
import org.jboss.jca.common.metadata.ds.DsXaPoolImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * A binding description for DataSourceDefinition annotations.
 * <p/>
 * The referenced datasource must be directly visible to the
 * component declaring the annotation.
 *
 * @author Jason T. Greene
 */
public class DataSourceDefinitionInjectionSource extends ResourceDefinitionInjectionSource {

    public static final String USER_PROP = "user";
    public static final String URL_PROP = "url";
    public static final String UPPERCASE_URL_PROP = "URL";
    public static final String TRANSACTIONAL_PROP = "transactional";
    public static final String SERVER_NAME_PROP = "serverName";
    public static final String PORT_NUMBER_PROP = "portNumber";
    public static final String PASSWORD_PROP = "password";
    public static final String MIN_POOL_SIZE_PROP = "minPoolSize";
    public static final String MAX_STATEMENTS_PROP = "maxStatements";
    public static final String MAX_IDLE_TIME_PROP = "maxIdleTime";
    public static final String LOGIN_TIMEOUT_PROP = "loginTimeout";
    public static final String ISOLATION_LEVEL_PROP = "isolationLevel";
    public static final String INITIAL_POOL_SIZE_PROP = "initialPoolSize";
    public static final String DESCRIPTION_PROP = "description";
    public static final String DATABASE_NAME_PROP = "databaseName";
    public static final String MAX_POOL_SIZE_PROP = "maxPoolSize";

    private String className;
    private String description;
    private String url;

    private String databaseName;
    private String serverName;
    private int portNumber = -1;

    private int loginTimeout = -1;

    private int isolationLevel = -1;
    private boolean transactional = true;

    private int initialPoolSize = -1;
    private int maxIdleTime = -1;
    private int maxPoolSize = -1;
    private int maxStatements = -1;
    private int minPoolSize = -1;

    private String user;
    private String password;

    public DataSourceDefinitionInjectionSource(final String jndiName) {
        super(jndiName);
    }

    public void getResourceValue(final ResolutionContext context, final ServiceBuilder<?> serviceBuilder, final DeploymentPhaseContext phaseContext, final Injector<ManagedReferenceFactory> injector) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        final Module module = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.MODULE);
        final EEModuleDescription eeModuleDescription = deploymentUnit.getAttachment(Attachments.EE_MODULE_DESCRIPTION);
        final String poolName = uniqueName(context, jndiName);
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoForEnvEntry(context.getApplicationName(), context.getModuleName(), context.getComponentName(), !context.isCompUsesModule(), jndiName);
        final DeploymentReflectionIndex reflectionIndex = deploymentUnit.getAttachment(org.jboss.as.server.deployment.Attachments.REFLECTION_INDEX);
        try {
            final Class<?> clazz = module.getClassLoader().loadClass(className);

            clearUnknownProperties(reflectionIndex, clazz, properties);
            populateProperties(reflectionIndex, clazz, properties);
            DsSecurityImpl dsSecurity = new DsSecurityImpl(user, password, null, false, null, null);

            if (XADataSource.class.isAssignableFrom(clazz) && transactional) {
                final DsXaPoolImpl xaPool = new DsXaPoolImpl(minPoolSize < 0 ? Defaults.MIN_POOL_SIZE : Integer.valueOf(minPoolSize),
                                                             initialPoolSize < 0 ? Defaults.INITIAL_POOL_SIZE : Integer.valueOf(initialPoolSize),
                                                             maxPoolSize < 1 ? Defaults.MAX_POOL_SIZE : Integer.valueOf(maxPoolSize),
                                                             Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY,
                                                             Defaults.IS_SAME_RM_OVERRIDE, Defaults.INTERLEAVING, Defaults.PAD_XID,
                                                             Defaults.WRAP_XA_RESOURCE, Defaults.NO_TX_SEPARATE_POOL, Boolean.FALSE, null, Defaults.FAIR, null);
                final ModifiableXaDataSource dataSource = new ModifiableXaDataSource(transactionIsolation(),
                        null, dsSecurity, null, null, null,
                        null, null, null, poolName, true,
                        jndiName, false, false, Defaults.CONNECTABLE, Defaults.TRACKING, Defaults.MCP, Defaults.ENLISTMENT_TRACE, properties,
                        className, null, null,
                        xaPool, null);
                final XaDataSourceService xds = new XaDataSourceService(bindInfo.getBinderServiceName().getCanonicalName(), bindInfo, module.getClassLoader());
                xds.getDataSourceConfigInjector().inject(dataSource);
                startDataSource(xds, bindInfo, eeModuleDescription, context, phaseContext.getServiceTarget(), serviceBuilder, injector);
            } else {
                final DsPoolImpl commonPool = new DsPoolImpl(minPoolSize < 0 ? Defaults.MIN_POOL_SIZE : Integer.valueOf(minPoolSize),
                                                             initialPoolSize < 0 ? Defaults.INITIAL_POOL_SIZE : Integer.valueOf(initialPoolSize),
                                                             maxPoolSize < 1 ? Defaults.MAX_POOL_SIZE : Integer.valueOf(maxPoolSize),
                                                             Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY, Boolean.FALSE, null, Defaults.FAIR, null);
                final ModifiableDataSource dataSource = new ModifiableDataSource(url, null, className, null, transactionIsolation(), properties,
                        null, dsSecurity, null, null, null, null, null, false, poolName, true, jndiName, Defaults.SPY, Defaults.USE_CCM,
                        transactional, Defaults.CONNECTABLE, Defaults.TRACKING, Defaults.MCP, Defaults.ENLISTMENT_TRACE, commonPool);
                final LocalDataSourceService ds = new LocalDataSourceService(bindInfo.getBinderServiceName().getCanonicalName(), bindInfo, module.getClassLoader());
                ds.getDataSourceConfigInjector().inject(dataSource);
                startDataSource(ds, bindInfo, eeModuleDescription, context, phaseContext.getServiceTarget(), serviceBuilder, injector);
            }

        } catch (Exception e) {
            throw new DeploymentUnitProcessingException(e);
        }
    }

    private void clearUnknownProperties(final DeploymentReflectionIndex reflectionIndex, final Class<?> dataSourceClass, final Map<String, String> props) {
        final Iterator<Map.Entry<String, String>> it = props.entrySet().iterator();
        while (it.hasNext()) {
            final Map.Entry<String, String> entry = it.next();
            String value = entry.getKey();
            if (value == null || "".equals(value)) {
                it.remove();
            } else {
                StringBuilder builder = new StringBuilder("set").append(entry.getKey());
                builder.setCharAt(3, Character.toUpperCase(entry.getKey().charAt(0)));
                final String methodName = builder.toString();
                final Class<?> paramType = value.getClass();
                final MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName, paramType);
                final Method setterMethod = ClassReflectionIndexUtil.findMethod(reflectionIndex, dataSourceClass, methodIdentifier);
                if (setterMethod == null) {
                    it.remove();
                    ConnectorLogger.DS_DEPLOYER_LOGGER.methodNotFoundOnDataSource(methodName, dataSourceClass);
                }
            }

        }
    }

    private String uniqueName(ResolutionContext context, final String jndiName) {
        StringBuilder name = new StringBuilder();
        name.append(context.getApplicationName() + "_");
        name.append(context.getModuleName() + "_");
        if (context.getComponentName() != null) {
            name.append(context.getComponentName() + "_");
        }
        name.append(jndiName);
        return name.toString();
    }

    private void startDataSource(final AbstractDataSourceService dataSourceService,
                                 final ContextNames.BindInfo bindInfo,
                                 final EEModuleDescription moduleDescription,
                                 final ResolutionContext context,
                                 final ServiceTarget serviceTarget,
                                 final ServiceBuilder valueSourceServiceBuilder, final Injector<ManagedReferenceFactory> injector) {

        final ServiceName dataSourceServiceName = AbstractDataSourceService.getServiceName(bindInfo);
        final ServiceBuilder<?> dataSourceServiceBuilder =
                Services.addServerExecutorDependency(
                        serviceTarget.addService(dataSourceServiceName, dataSourceService),
                        dataSourceService.getExecutorServiceInjector())
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, dataSourceService.getMdrInjector())
                .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class, dataSourceService.getRaRepositoryInjector())
                .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(DEFAULT_NAME))
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        dataSourceService.getTransactionIntegrationInjector())
                .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                        dataSourceService.getManagementRepositoryInjector())
                .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, dataSourceService.getCcmInjector())
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        dataSourceService.getDriverRegistryInjector()).addDependency(NamingService.SERVICE_NAME);

        // We don't need to inject legacy security subsystem services. They are only used with a configured legacy
        // security domain, and the annotation does not support configuring that
//        if(securityEnabled) {
//            dataSourceServiceBuilder.addDependency(SimpleSecurityManagerService.SERVICE_NAME, ServerSecurityManager.class,
//                    dataSourceService.getServerSecurityManager());
//            dataSourceServiceBuilder.addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
//                    dataSourceService.getSubjectFactoryInjector());
//        }


        final DataSourceReferenceFactoryService referenceFactoryService = new DataSourceReferenceFactoryService();
        final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                .append(bindInfo.getBinderServiceName());
        final ServiceBuilder<?> referenceBuilder = serviceTarget.addService(referenceFactoryServiceName,
                referenceFactoryService).addDependency(dataSourceServiceName, javax.sql.DataSource.class,
                referenceFactoryService.getDataSourceInjector());

        final BinderService binderService = new BinderService(bindInfo.getBindName(), this);
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector())
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {
                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                if (isTransactional()) {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundDataSource(jndiName);
                                } else {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundNonJTADataSource(jndiName);
                                }
                                break;
                            }
                            case DOWN: {
                                if (isTransactional()) {
                                    SUBSYSTEM_DATASOURCES_LOGGER.unboundDataSource(jndiName);
                                } else {
                                    SUBSYSTEM_DATASOURCES_LOGGER.unBoundNonJTADataSource(jndiName);
                                }
                                break;
                            }
                            case REMOVED: {
                                SUBSYSTEM_DATASOURCES_LOGGER.debugf("Removed JDBC Data-source [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });

        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();

        valueSourceServiceBuilder.addDependency(bindInfo.getBinderServiceName(), ManagedReferenceFactory.class, injector);
    }

    private TransactionIsolation transactionIsolation() {
        switch (isolationLevel) {
            case Connection.TRANSACTION_NONE:
                return TransactionIsolation.TRANSACTION_NONE;
            case Connection.TRANSACTION_READ_COMMITTED:
                return TransactionIsolation.TRANSACTION_READ_COMMITTED;
            case Connection.TRANSACTION_READ_UNCOMMITTED:
                return TransactionIsolation.TRANSACTION_READ_UNCOMMITTED;
            case Connection.TRANSACTION_REPEATABLE_READ:
                return TransactionIsolation.TRANSACTION_REPEATABLE_READ;
            case Connection.TRANSACTION_SERIALIZABLE:
                return TransactionIsolation.TRANSACTION_SERIALIZABLE;
            default:
                return TransactionIsolation.TRANSACTION_READ_COMMITTED;
        }
    }

    private void populateProperties(final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> dataSourceClass, final Map<String, String> properties) {
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, DESCRIPTION_PROP, description);
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, URL_PROP, url);
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, UPPERCASE_URL_PROP, url);
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, DATABASE_NAME_PROP, databaseName);
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, SERVER_NAME_PROP, serverName);
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, PORT_NUMBER_PROP, Integer.valueOf(portNumber));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, LOGIN_TIMEOUT_PROP, Integer.valueOf(loginTimeout));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, ISOLATION_LEVEL_PROP, Integer.valueOf(isolationLevel));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, TRANSACTIONAL_PROP, Boolean.valueOf(transactional));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, INITIAL_POOL_SIZE_PROP, Integer.valueOf(initialPoolSize));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, MAX_IDLE_TIME_PROP, Integer.valueOf(maxIdleTime));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, MAX_POOL_SIZE_PROP, Integer.valueOf(maxPoolSize));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, MAX_STATEMENTS_PROP, Integer.valueOf(maxStatements));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, MIN_POOL_SIZE_PROP, Integer.valueOf(minPoolSize));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, INITIAL_POOL_SIZE_PROP, Integer.valueOf(minPoolSize));
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, USER_PROP, user);
        setProperty(deploymentReflectionIndex, dataSourceClass, properties, PASSWORD_PROP, password);
    }

    private void setProperty(final DeploymentReflectionIndex deploymentReflectionIndex, final Class<?> dataSourceClass, final Map<String, String> properties, final String name, final Object value) {
        // Ignore defaulted values
        if (value == null || "".equals(value)) return;
        if (value instanceof Integer && ((Integer) value).intValue() == -1) return;
        StringBuilder builder = new StringBuilder("set").append(name);
        builder.setCharAt(3, Character.toUpperCase(name.charAt(0)));
        final String methodName = builder.toString();
        final Class<?> paramType = value.getClass();
        MethodIdentifier methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName, paramType);
        Method setterMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, dataSourceClass, methodIdentifier);
        if (setterMethod != null) {
            properties.put(name, value.toString());
        } else if (paramType == Integer.class) {
            //if this is an Integer also look for int setters (WFLY-1364)
            methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName, int.class);
            setterMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, dataSourceClass, methodIdentifier);
            if (setterMethod != null) {
                properties.put(name, value.toString());
            }
        } else if (paramType == Boolean.class) {
            methodIdentifier = MethodIdentifier.getIdentifier(void.class, methodName, boolean.class);
            setterMethod = ClassReflectionIndexUtil.findMethod(deploymentReflectionIndex, dataSourceClass, methodIdentifier);
            if (setterMethod != null) {
                properties.put(name, value.toString());
            }
        }
    }

    public String getClassName() {
        return className;
    }


    public void setClassName(String className) {
        this.className = className;
    }


    public String getDescription() {
        return description;
    }


    public void setDescription(String description) {
        this.description = description;
    }


    public String getUrl() {
        return url;
    }


    public void setUrl(String url) {
        this.url = url;
    }


    public String getDatabaseName() {
        return databaseName;
    }


    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }


    public String getServerName() {
        return serverName;
    }


    public void setServerName(String serverName) {
        this.serverName = serverName;
    }


    public int getPortNumber() {
        return portNumber;
    }


    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }


    public int getLoginTimeout() {
        return loginTimeout;
    }


    public void setLoginTimeout(int loginTimeout) {
        this.loginTimeout = loginTimeout;
    }


    public int getIsolationLevel() {
        return isolationLevel;
    }


    public void setIsolationLevel(int isolationLevel) {
        this.isolationLevel = isolationLevel;
    }


    public boolean isTransactional() {
        return transactional;
    }


    public void setTransactional(boolean transactional) {
        this.transactional = transactional;
    }


    public int getInitialPoolSize() {
        return initialPoolSize;
    }


    public void setInitialPoolSize(int initialPoolSize) {
        this.initialPoolSize = initialPoolSize;
    }


    public int getMaxIdleTime() {
        return maxIdleTime;
    }


    public void setMaxIdleTime(int maxIdleTime) {
        this.maxIdleTime = maxIdleTime;
    }


    public int getMaxPoolSize() {
        return maxPoolSize;
    }


    public void setMaxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
    }


    public int getMaxStatements() {
        return maxStatements;
    }


    public void setMaxStatements(int maxStatements) {
        this.maxStatements = maxStatements;
    }


    public int getMinPoolSize() {
        return minPoolSize;
    }

    public void setMinPoolSize(int minPoolSize) {
        this.minPoolSize = minPoolSize;
    }


    public String getUser() {
        return user;
    }


    public void setUser(String user) {
        this.user = user;
    }


    public String getPassword() {
        return password;
    }


    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        DataSourceDefinitionInjectionSource that = (DataSourceDefinitionInjectionSource) o;

        if (initialPoolSize != that.initialPoolSize) return false;
        if (isolationLevel != that.isolationLevel) return false;
        if (loginTimeout != that.loginTimeout) return false;
        if (maxIdleTime != that.maxIdleTime) return false;
        if (maxPoolSize != that.maxPoolSize) return false;
        if (maxStatements != that.maxStatements) return false;
        if (minPoolSize != that.minPoolSize) return false;
        if (portNumber != that.portNumber) return false;
        if (transactional != that.transactional) return false;
        if (className != null ? !className.equals(that.className) : that.className != null) return false;
        if (databaseName != null ? !databaseName.equals(that.databaseName) : that.databaseName != null) return false;
        if (description != null ? !description.equals(that.description) : that.description != null) return false;
        if (password != null ? !password.equals(that.password) : that.password != null) return false;
        if (serverName != null ? !serverName.equals(that.serverName) : that.serverName != null) return false;
        if (url != null ? !url.equals(that.url) : that.url != null) return false;
        if (user != null ? !user.equals(that.user) : that.user != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (className != null ? className.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (url != null ? url.hashCode() : 0);
        result = 31 * result + (databaseName != null ? databaseName.hashCode() : 0);
        result = 31 * result + (serverName != null ? serverName.hashCode() : 0);
        result = 31 * result + portNumber;
        result = 31 * result + loginTimeout;
        result = 31 * result + isolationLevel;
        result = 31 * result + (transactional ? 1 : 0);
        result = 31 * result + initialPoolSize;
        result = 31 * result + maxIdleTime;
        result = 31 * result + maxPoolSize;
        result = 31 * result + maxStatements;
        result = 31 * result + minPoolSize;
        result = 31 * result + (user != null ? user.hashCode() : 0);
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }
}
