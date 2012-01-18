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

package org.jboss.as.connector.deployers.processors;

import java.sql.Driver;

import org.jboss.as.connector.ConnectorLogger;
import org.jboss.as.connector.ConnectorMessages;
import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.connector.registry.DriverRegistry;
import org.jboss.as.connector.subsystems.datasources.AbstractDataSourceService;
import org.jboss.as.connector.subsystems.datasources.DataSourceReferenceFactoryService;
import org.jboss.as.connector.subsystems.datasources.LocalDataSourceService;
import org.jboss.as.connector.subsystems.datasources.ModifiableDataSource;
import org.jboss.as.connector.subsystems.datasources.ModifiableXaDataSource;
import org.jboss.as.connector.subsystems.datasources.XaDataSourceService;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.deployment.DeploymentPhaseContext;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.DeploymentUnitProcessingException;
import org.jboss.as.server.deployment.DeploymentUnitProcessor;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.DataSources;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.security.SubjectFactory;

import static org.jboss.as.connector.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;

/**
 * Picks up -ds.xml deployments
 *
 * @author <a href="mailto:jesper.pedersen@jboss.org">Jesper Pedersen</a>
 */
public class DsXmlDeploymentInstallProcessor implements DeploymentUnitProcessor {

    /**
     * Construct a new instance.
     */
    public DsXmlDeploymentInstallProcessor() {
    }

    /**
     * Process a deployment for standard ra deployment files. Will parse the xml
     * file and attach an configuration discovered during processing.
     *
     * @param phaseContext the deployment unit context
     * @throws org.jboss.as.server.deployment.DeploymentUnitProcessingException
     *
     */
    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        final DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();

        DataSources dataSources = deploymentUnit.getAttachment(DsXmlDeploymentParsingProcessor.DATA_SOURCES_ATTACHMENT_KEY);

        if (dataSources != null) {
            if (dataSources.getDrivers() != null && dataSources.getDrivers().size() > 0) {
                ConnectorLogger.DS_DEPLOYER_LOGGER.driversElementNotSupported(deploymentUnit.getName());
            }

            ServiceTarget serviceTarget = phaseContext.getServiceTarget();
            ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();

            if (dataSources.getDataSource() != null && dataSources.getDataSource().size() > 0) {
                for (DataSource ds : dataSources.getDataSource()) {
                    if (ds.isEnabled() && ds.getDriver() != null) {
                        try {
                            final String jndiName = cleanupJavaContext(ds.getJndiName());
                            LocalDataSourceService lds = new LocalDataSourceService(jndiName);
                            lds.getDataSourceConfigInjector().inject(buildDataSource(ds));

                            startDataSource(lds, jndiName, ds.getDriver(), serviceTarget, verificationHandler);
                        } catch (Exception e) {
                            throw ConnectorMessages.MESSAGES.exceptionDeployingDatasource(e, ds.getJndiName());
                        }
                    } else {
                        ConnectorLogger.DS_DEPLOYER_LOGGER.debugf("Ignoring: %s", ds.getJndiName());
                    }
                }
            }

            if (dataSources.getXaDataSource() != null && dataSources.getXaDataSource().size() > 0) {
                for (XaDataSource xads : dataSources.getXaDataSource()) {
                    if (xads.isEnabled() && xads.getDriver() != null) {
                        try {
                            String jndiName = cleanupJavaContext(xads.getJndiName());
                            XaDataSourceService xds = new XaDataSourceService(jndiName);
                            xds.getDataSourceConfigInjector().inject(buildXaDataSource(xads));

                            startDataSource(xds, jndiName, xads.getDriver(), serviceTarget, verificationHandler);

                        } catch (Exception e) {
                            throw ConnectorMessages.MESSAGES.exceptionDeployingDatasource(e, xads.getJndiName());
                        }
                    } else {
                        ConnectorLogger.DS_DEPLOYER_LOGGER.debugf("Ignoring %s", xads.getJndiName());
                    }
                }
            }
        }

    }


    public void undeploy(final DeploymentUnit context) {
    }

    private ModifiableDataSource buildDataSource(DataSource ds) throws org.jboss.jca.common.api.validator.ValidateException {
        return new ModifiableDataSource(ds.getConnectionUrl(),
                ds.getDriverClass(), ds.getDataSourceClass(), ds.getDriver(),
                ds.getTransactionIsolation(), ds.getConnectionProperties(), ds.getTimeOut(),
                ds.getSecurity(), ds.getStatement(), ds.getValidation(),
                ds.getUrlDelimiter(), ds.getUrlSelectorStrategyClassName(), ds.getNewConnectionSql(),
                ds.isUseJavaContext(), ds.getPoolName(), ds.isEnabled(), ds.getJndiName(),
                ds.isSpy(), ds.isUseCcm(), ds.isJTA(), ds.getPool());
    }

    private ModifiableXaDataSource buildXaDataSource(XaDataSource xads) throws org.jboss.jca.common.api.validator.ValidateException {
        return new ModifiableXaDataSource(xads.getTransactionIsolation(),
                xads.getTimeOut(), xads.getSecurity(),
                xads.getStatement(), xads.getValidation(),
                xads.getUrlDelimiter(), xads.getUrlSelectorStrategyClassName(),
                xads.isUseJavaContext(), xads.getPoolName(), xads.isEnabled(), xads.getJndiName(),
                xads.isSpy(), xads.isUseCcm(),
                xads.getXaDataSourceProperty(), xads.getXaDataSourceClass(), xads.getDriver(),
                xads.getNewConnectionSql(), xads.getXaPool(), xads.getRecovery());
    }

    private void startDataSource(final AbstractDataSourceService dataSourceService,
                                 final String jndiName,
                                 final String driverName,
                                 final ServiceTarget serviceTarget,
                                 final ServiceVerificationHandler verificationHandler) {

        final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
        final ServiceBuilder<?> dataSourceServiceBuilder = serviceTarget
                .addService(dataSourceServiceName, dataSourceService)
                .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                        dataSourceService.getTransactionIntegrationInjector())
                .addDependency(ConnectorServices.MANAGEMENT_REPOSISTORY_SERVICE, ManagementRepository.class,
                        dataSourceService.getmanagementRepositoryInjector())
                .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                        dataSourceService.getSubjectFactoryInjector())
                .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, dataSourceService.getCcmInjector())
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        dataSourceService.getDriverRegistryInjector()).addDependency(NamingService.SERVICE_NAME);

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

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector())
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new AbstractServiceListener<Object>() {
                    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                SUBSYSTEM_DATASOURCES_LOGGER.boundDataSource(jndiName);
                                break;
                            }
                            case START_REQUESTED_to_DOWN: {
                                SUBSYSTEM_DATASOURCES_LOGGER.unboundDataSource(jndiName);
                                break;
                            }
                            case REMOVING_to_REMOVED: {
                                SUBSYSTEM_DATASOURCES_LOGGER.debugf("Removed JDBC Data-source [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });

        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install();
        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install();
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE).addListener(verificationHandler).install();
    }

    static String cleanupJavaContext(String jndiName) {
        String bindName;
        if (jndiName.startsWith("java:/")) {
            bindName = jndiName.substring(6);
        } else if (jndiName.startsWith("java:")) {
            bindName = jndiName.substring(5);
        } else {
            bindName = jndiName;
        }
        return bindName;
    }
}
