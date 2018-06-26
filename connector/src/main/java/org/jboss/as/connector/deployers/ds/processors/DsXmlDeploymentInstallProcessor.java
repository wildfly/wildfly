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

package org.jboss.as.connector.deployers.ds.processors;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.metadata.api.common.SecurityMetadata;
import org.jboss.as.connector.metadata.api.ds.DsSecurity;
import org.jboss.as.connector.services.datasources.statistics.DataSourceStatisticsService;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.connector.subsystems.datasources.CommonDeploymentService;
import org.jboss.as.connector.subsystems.datasources.Constants;
import org.jboss.as.connector.subsystems.datasources.DataSourceReferenceFactoryService;
import org.jboss.as.connector.subsystems.datasources.DataSourcesExtension;
import org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders;
import org.jboss.as.connector.subsystems.datasources.LocalDataSourceService;
import org.jboss.as.connector.subsystems.datasources.ModifiableDataSource;
import org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource;
import org.jboss.as.connector.subsystems.datasources.Util;
import org.jboss.as.connector.subsystems.datasources.XMLDataSourceRuntimeHandler;
import org.jboss.as.connector.subsystems.datasources.XMLXaDataSourceRuntimeHandler;
import org.jboss.as.connector.subsystems.datasources.XaDataSourceService;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.deployment.SecurityAttachments;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentModelUtils;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentResourceSupport;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.DsXaPool;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.common.metadata.ds.DsXaPoolImpl;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

/**
 * Picks up -ds.xml deployments
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsXmlDeploymentInstallProcessor implements DeploymentUnitProcessor {

    private static final String DATA_SOURCE = "data-source";
    private static final String XA_DATA_SOURCE = "xa-data-source";
    private static final String CONNECTION_PROPERTIES = "connection-properties";
    private static final String XA_CONNECTION_PROPERTIES = "xa-datasource-properties";

    private static final PathAddress SUBSYSTEM_ADDRESS = PathAddress.pathAddress(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME));
    private static final PathAddress DATASOURCE_ADDRESS;
    private static final PathAddress XA_DATASOURCE_ADDRESS;

    static {
        XA_DATASOURCE_ADDRESS = SUBSYSTEM_ADDRESS.append(PathElement.pathElement(XA_DATA_SOURCE));
        DATASOURCE_ADDRESS = SUBSYSTEM_ADDRESS.append(PathElement.pathElement(DATA_SOURCE));
    }

    /**
     * Process a deployment for standard ra deployment files. Will parse the xml
     * file and attach a configuration discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        final List<DataSources> dataSourcesList = deploymentUnit.getAttachmentList(DsXmlDeploymentParsingProcessor.DATA_SOURCES_ATTACHMENT_KEY);

        final boolean legacySecurityPresent = phaseContext.getDeploymentUnit().hasAttachment(SecurityAttachments.SECURITY_ENABLED);

        for(DataSources dataSources : dataSourcesList) {
            if (dataSources.getDrivers() != null && dataSources.getDrivers().size() > 0) {
                ConnectorLogger.DS_DEPLOYER_LOGGER.driversElementNotSupported(deploymentUnit.getName());
            }

            ServiceTarget serviceTarget = phaseContext.getServiceTarget();

            if (dataSources.getDataSource() != null && dataSources.getDataSource().size() > 0) {
                for (int i = 0; i < dataSources.getDataSource().size(); i++) {
                    DataSource ds = (DataSource)dataSources.getDataSource().get(i);
                    if (ds.isEnabled() && ds.getDriver() != null) {
                        try {
                            final String jndiName = Util.cleanJndiName(ds.getJndiName(), ds.isUseJavaContext());
                            LocalDataSourceService lds = new LocalDataSourceService(jndiName, ContextNames.bindInfoFor(jndiName));
                            lds.getDataSourceConfigInjector().inject(buildDataSource(ds));
                            final String dsName = ds.getJndiName();
                            final PathAddress addr = getDataSourceAddress(dsName, deploymentUnit, false);
                            installManagementModel(ds, deploymentUnit, addr);
                            // TODO why have we been ignoring a configured legacy security domain but no legacy security present?
                            boolean useLegacySecurity = legacySecurityPresent && isLegacySecurityRequired(ds.getSecurity());
                            startDataSource(lds, jndiName, ds.getDriver(), serviceTarget,
                                    getRegistration(false, deploymentUnit), getResource(dsName, false, deploymentUnit), dsName, useLegacySecurity, ds.isJTA());
                        } catch (Exception e) {
                            throw ConnectorLogger.ROOT_LOGGER.exceptionDeployingDatasource(e, ds.getJndiName());
                        }
                    } else {
                        ConnectorLogger.DS_DEPLOYER_LOGGER.debugf("Ignoring: %s", ds.getJndiName());
                    }
                }
            }

            if (dataSources.getXaDataSource() != null && dataSources.getXaDataSource().size() > 0) {
               for (int i = 0; i < dataSources.getXaDataSource().size(); i++) {
                    XaDataSource xads = (XaDataSource)dataSources.getXaDataSource().get(i);
                    if (xads.isEnabled() && xads.getDriver() != null) {
                        try {
                            String jndiName = Util.cleanJndiName(xads.getJndiName(), xads.isUseJavaContext());
                            XaDataSourceService xds = new XaDataSourceService(jndiName, ContextNames.bindInfoFor(jndiName));
                            xds.getDataSourceConfigInjector().inject(buildXaDataSource(xads));
                            final String dsName = xads.getJndiName();
                            final PathAddress addr = getDataSourceAddress(dsName, deploymentUnit, true);
                            installManagementModel(xads, deploymentUnit, addr);
                            final Credential credential = xads.getRecovery() == null? null: xads.getRecovery().getCredential();
                            // TODO why have we been ignoring a configured legacy security domain but no legacy security present?
                            boolean useLegacySecurity = legacySecurityPresent && (isLegacySecurityRequired(xads.getSecurity())
                                                        || isLegacySecurityRequired(credential));
                            startDataSource(xds, jndiName, xads.getDriver(), serviceTarget,
                                    getRegistration(true, deploymentUnit), getResource(dsName, true, deploymentUnit), dsName, useLegacySecurity, true);

                        } catch (Exception e) {
                            throw ConnectorLogger.ROOT_LOGGER.exceptionDeployingDatasource(e, xads.getJndiName());
                        }
                    } else {
                        ConnectorLogger.DS_DEPLOYER_LOGGER.debugf("Ignoring %s", xads.getJndiName());
                    }
                }
            }
        }

    }

    private void installManagementModel(final DataSource ds, final DeploymentUnit deploymentUnit, final PathAddress addr) {
        XMLDataSourceRuntimeHandler.INSTANCE.registerDataSource(addr, ds);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubModel(DataSourcesExtension.SUBSYSTEM_NAME, addr.getLastElement());
        if (ds.getConnectionProperties() != null) {
            for (final Map.Entry<String, String> prop : ds.getConnectionProperties().entrySet()) {
                PathAddress registration = PathAddress.pathAddress(addr.getLastElement(), PathElement.pathElement(CONNECTION_PROPERTIES, prop.getKey()));
                deploymentResourceSupport.getDeploymentSubModel(DataSourcesExtension.SUBSYSTEM_NAME, registration);
            }
        }
    }


    private void installManagementModel(final XaDataSource ds, final DeploymentUnit deploymentUnit, final PathAddress addr) {
        XMLXaDataSourceRuntimeHandler.INSTANCE.registerDataSource(addr, ds);
        final DeploymentResourceSupport deploymentResourceSupport = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_RESOURCE_SUPPORT);
        deploymentResourceSupport.getDeploymentSubModel(DataSourcesExtension.SUBSYSTEM_NAME, addr.getLastElement());
        if (ds.getXaDataSourceProperty() != null) {
            for (final Map.Entry<String, String> prop : ds.getXaDataSourceProperty().entrySet()) {
                PathAddress registration = PathAddress.pathAddress(addr.getLastElement(), PathElement.pathElement(XA_CONNECTION_PROPERTIES, prop.getKey()));
                deploymentResourceSupport.getDeploymentSubModel(DataSourcesExtension.SUBSYSTEM_NAME, registration);
            }
        }
    }

    private void undeployDataSource(final DataSource ds, final DeploymentUnit deploymentUnit) {
        final PathAddress addr = getDataSourceAddress(ds.getJndiName(), deploymentUnit, false);
        XMLDataSourceRuntimeHandler.INSTANCE.unregisterDataSource(addr);
    }

    private void undeployXaDataSource(final XaDataSource ds, final DeploymentUnit deploymentUnit) {
        final PathAddress addr = getDataSourceAddress(ds.getJndiName(), deploymentUnit, true);
        XMLXaDataSourceRuntimeHandler.INSTANCE.unregisterDataSource(addr);
    }

    @Override
    public void undeploy(final DeploymentUnit deploymentUnit) {
        final List<DataSources> dataSourcesList = deploymentUnit.getAttachmentList(DsXmlDeploymentParsingProcessor.DATA_SOURCES_ATTACHMENT_KEY);

        for (final DataSources dataSources : dataSourcesList) {
            if (dataSources.getDataSource() != null) {
                for (int i = 0; i < dataSources.getDataSource().size(); i++) {
                    DataSource ds = dataSources.getDataSource().get(i);
                    undeployDataSource(ds, deploymentUnit);
                }
            }
            if (dataSources.getXaDataSource() != null) {
                for (int i = 0; i < dataSources.getXaDataSource().size(); i++) {
                    XaDataSource xads = dataSources.getXaDataSource().get(i);
                    undeployXaDataSource(xads, deploymentUnit);
                }
            }
        }

        deploymentUnit.removeAttachment(DsXmlDeploymentParsingProcessor.DATA_SOURCES_ATTACHMENT_KEY);
    }


    private ModifiableDataSource buildDataSource(DataSource ds) throws org.jboss.jca.common.api.validator.ValidateException {
        assert ds.getSecurity() == null || ds.getSecurity() instanceof DsSecurity;
        return new ModifiableDataSource(ds.getConnectionUrl(),
                ds.getDriverClass(), ds.getDataSourceClass(), ds.getDriver(),
                ds.getTransactionIsolation(), ds.getConnectionProperties(), ds.getTimeOut(),
                (DsSecurity) ds.getSecurity(), ds.getStatement(), ds.getValidation(),
                ds.getUrlDelimiter(), ds.getUrlSelectorStrategyClassName(), ds.getNewConnectionSql(),
                ds.isUseJavaContext(), ds.getPoolName(), ds.isEnabled(), ds.getJndiName(),
                ds.isSpy(), ds.isUseCcm(), ds.isJTA(), ds.isConnectable(), ds.isTracking(), ds.getMcp(), ds.isEnlistmentTrace(), ds.getPool());
    }

    private ModifiableXaDataSource buildXaDataSource(XaDataSource xads) throws org.jboss.jca.common.api.validator.ValidateException {
        final DsXaPool xaPool;
        if (xads.getXaPool() == null) {
            xaPool = new DsXaPoolImpl(Defaults.MIN_POOL_SIZE, Defaults.INITIAL_POOL_SIZE, Defaults.MAX_POOL_SIZE, Defaults.PREFILL, Defaults.USE_STRICT_MIN, Defaults.FLUSH_STRATEGY,
                                      Defaults.IS_SAME_RM_OVERRIDE, Defaults.INTERLEAVING, Defaults.PAD_XID, Defaults.WRAP_XA_RESOURCE, Defaults.NO_TX_SEPARATE_POOL, Defaults.ALLOW_MULTIPLE_USERS, null, Defaults.FAIR, null);
        } else {
            final DsXaPool p = xads.getXaPool();
            xaPool = new DsXaPoolImpl(getDef(p.getMinPoolSize(), Defaults.MIN_POOL_SIZE), getDef(p.getInitialPoolSize(), Defaults.INITIAL_POOL_SIZE), getDef(p.getMaxPoolSize(), Defaults.MAX_POOL_SIZE), getDef(p.isPrefill(), Defaults.PREFILL),
                    getDef(p.isUseStrictMin(), Defaults.USE_STRICT_MIN), getDef(p.getFlushStrategy(), Defaults.FLUSH_STRATEGY), getDef(p.isSameRmOverride(),
                    Defaults.IS_SAME_RM_OVERRIDE), getDef(p.isInterleaving(), Defaults.INTERLEAVING), getDef(p.isPadXid(), Defaults.PAD_XID)
                    , getDef(p.isWrapXaResource(), Defaults.WRAP_XA_RESOURCE), getDef(p.isNoTxSeparatePool(), Defaults.NO_TX_SEPARATE_POOL), getDef(p.isAllowMultipleUsers(), Defaults.ALLOW_MULTIPLE_USERS),
                    p.getCapacity(), getDef(p.isFair(), Defaults.FAIR), p.getConnectionListener());
        }


        return new ModifiableXaDataSource(xads.getTransactionIsolation(),
                xads.getTimeOut(), xads.getSecurity(),
                xads.getStatement(), xads.getValidation(),
                xads.getUrlDelimiter(), xads.getUrlProperty(), xads.getUrlSelectorStrategyClassName(),
                xads.isUseJavaContext(), xads.getPoolName(), xads.isEnabled(), xads.getJndiName(),
                xads.isSpy(), xads.isUseCcm(), xads.isConnectable(), xads.isTracking(),
                xads.getMcp(), xads.isEnlistmentTrace(),
                xads.getXaDataSourceProperty(), xads.getXaDataSourceClass(), xads.getDriver(),
                xads.getNewConnectionSql(), xaPool, xads.getRecovery());
    }

    private <T> T getDef(T value, T def) {
        return value != null ? value : def;
    }


    private void startDataSource(final AbstractDataSourceService dataSourceService,
                                 final String jndiName,
                                 final String driverName,
                                 final ServiceTarget serviceTarget,
                                 final ManagementResourceRegistration registration,
                                 final Resource resource,
                                 final String managementName,
                                 boolean requireLegacySecurity,
                                 final boolean isTransactional) {

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);

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

        if (requireLegacySecurity) {
            dataSourceServiceBuilder.addDependency(SimpleSecurityManagerService.SERVICE_NAME, ServerSecurityManager.class,
                    dataSourceService.getServerSecurityManager());
            dataSourceServiceBuilder.addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                    dataSourceService.getSubjectFactoryInjector());
        }

        //Register an empty override model regardless of we're enabled or not - the statistics listener will add the relevant childresources
        if (registration.isAllowsOverride()) {
            ManagementResourceRegistration overrideRegistration = registration.getOverrideModel(managementName);
            if (overrideRegistration == null || overrideRegistration.isAllowsOverride()) {
                overrideRegistration = registration.registerOverrideModel(managementName, DataSourcesSubsystemProviders.OVERRIDE_DS_DESC);
            }
            DataSourceStatisticsService statsService = new DataSourceStatisticsService(registration, false );
                            serviceTarget.addService(dataSourceServiceName.append(Constants.STATISTICS), statsService)
                                    .addDependency(dataSourceServiceName)
                                    .addDependency(CommonDeploymentService.getServiceName(bindInfo), CommonDeployment.class, statsService.getCommonDeploymentInjector())
                                    .setInitialMode(ServiceController.Mode.PASSIVE)
                                    .install();
            DataSourceStatisticsService.registerStatisticsResources(resource);
        } // else should probably throw an ISE or something

        final ServiceName driverServiceName = ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_"));
        if (driverServiceName != null) {
            dataSourceServiceBuilder.addDependency(driverServiceName, Driver.class,
                    dataSourceService.getDriverInjector());
        }

        final DataSourceReferenceFactoryService referenceFactoryService = new DataSourceReferenceFactoryService();
        final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                .append(jndiName);
        final ServiceBuilder<?> referenceBuilder = serviceTarget.addService(referenceFactoryServiceName,
                referenceFactoryService).addDependency(dataSourceServiceName, javax.sql.DataSource.class,
                referenceFactoryService.getDataSourceInjector());

        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector())
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {
                    public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                if (isTransactional) {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundDataSource(jndiName);
                                } else {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundNonJTADataSource(jndiName);
                                }
                                break;
                            }
                            case DOWN: {
                                if (isTransactional) {
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
    }

    private static PathAddress getDataSourceAddress(final String jndiName, DeploymentUnit deploymentUnit, boolean xa) {
        List<PathElement> elements = new ArrayList<PathElement>();
        if (deploymentUnit.getParent() == null) {
            elements.add(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, deploymentUnit.getName()));
        } else {
            elements.add(PathElement.pathElement(ModelDescriptionConstants.DEPLOYMENT, deploymentUnit.getParent().getName()));
            elements.add(PathElement.pathElement(ModelDescriptionConstants.SUBDEPLOYMENT, deploymentUnit.getName()));
        }
        elements.add(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, DataSourcesExtension.SUBSYSTEM_NAME));
        if (xa) {
            elements.add(PathElement.pathElement(XA_DATA_SOURCE, jndiName));
        } else {
            elements.add(PathElement.pathElement(DATA_SOURCE, jndiName));
        }
        return PathAddress.pathAddress(elements);
    }

    static Resource getOrCreate(final Resource parent, final PathAddress address) {
        Resource current = parent;
        for (final PathElement element : address) {
            synchronized (current) {
                if (current.hasChild(element)) {
                    current = current.requireChild(element);
                } else {
                    final Resource resource = Resource.Factory.create();
                    current.registerChild(element, resource);
                    current = resource;
                }
            }
        }
        return current;
    }

    private Resource getResource(final String dsName, final boolean xa, final DeploymentUnit unit) {
        final Resource root = unit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        final String key = xa ? XA_DATA_SOURCE : DATA_SOURCE;
        final PathAddress address = PathAddress.pathAddress(PathElement.pathElement(key, dsName));
        synchronized (root) {
            final Resource subsystem = getOrCreate(root, SUBSYSTEM_ADDRESS);
            return getOrCreate(subsystem, address);
        }
    }

    private ManagementResourceRegistration getRegistration(final boolean xa, final DeploymentUnit unit) {
        final Resource root = unit.getAttachment(DeploymentModelUtils.DEPLOYMENT_RESOURCE);
        synchronized (root) {
            ManagementResourceRegistration registration = unit.getAttachment(DeploymentModelUtils.MUTABLE_REGISTRATION_ATTACHMENT);
            final PathAddress address = xa ? XA_DATASOURCE_ADDRESS : DATASOURCE_ADDRESS;
            ManagementResourceRegistration subModel = registration.getSubModel(address);
            if (subModel == null) {
                throw new IllegalStateException(address.toString());
            }
            return subModel;
        }
    }

    private static boolean isLegacySecurityRequired(org.jboss.jca.common.api.metadata.common.SecurityMetadata config) {
        boolean result = config != null && config instanceof SecurityMetadata && !((SecurityMetadata) config).isElytronEnabled();
        if (result) {
            String domain = config.resolveSecurityDomain();
            result = domain != null && domain.trim().length() > 0;
        }
        return result;
    }
}
