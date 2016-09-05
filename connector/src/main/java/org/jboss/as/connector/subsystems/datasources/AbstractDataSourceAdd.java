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

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.from;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.xaFrom;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import javax.sql.DataSource;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.connector.services.datasources.statistics.DataSourceStatisticsService;
import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.core.security.ServerSecurityManager;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.as.security.service.SimpleSecurityManagerService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.mdr.MetadataRepository;
import org.jboss.jca.core.spi.rar.ResourceAdapterRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.jca.deployers.common.CommonDeployment;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueInjectionService;
import org.jboss.security.SubjectFactory;

/**
 * Abstract operation handler responsible for adding a DataSource.
 *
 * @author John Bailey
 */
public abstract class AbstractDataSourceAdd extends AbstractAddStepHandler {

    AbstractDataSourceAdd(Collection<AttributeDefinition> attributes) {
        super(Capabilities.DATA_SOURCE_CAPABILITY, attributes);
    }


    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        DataSourceStatisticsService.registerStatisticsResources(resource);
        super.populateModel(context, operation, resource);

    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final boolean enabled = ENABLED.resolveModelAttribute(context, model).asBoolean();
        if (enabled) {
            firstRuntimeStep(context, operation, model);
            context.addStep(new OperationStepHandler() {
                @Override
                public void execute(OperationContext operationContext, ModelNode modelNode) throws OperationFailedException {
                    secondRuntimeStep(context, operation, context.getResourceRegistrationForUpdate(), model, isXa());
                }
            }, OperationContext.Stage.RUNTIME);


        }
    }

     void firstRuntimeStep(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final boolean jta = JTA.resolveModelAttribute(context, operation).asBoolean();
        final String dsName = context.getCurrentAddressValue();// The STATISTICS_ENABLED.resolveModelAttribute(context, model) call should remain as it serves to validate that any
        // expression in the model can be resolved to a correct value.
        @SuppressWarnings("unused")
        final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        final ServiceTarget serviceTarget = context.getServiceTarget();


        ModelNode node = DATASOURCE_DRIVER.resolveModelAttribute(context, model);

        final String driverName = node.asString();
        final ServiceName driverServiceName = ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_"));


        ValueInjectionService<Driver> driverDemanderService = new ValueInjectionService<Driver>();

        final ServiceName driverDemanderServiceName = ServiceName.JBOSS.append("driver-demander").append(jndiName);
        final ServiceBuilder<?> driverDemanderBuilder = serviceTarget
                .addService(driverDemanderServiceName, driverDemanderService)
                .addDependency(driverServiceName, Driver.class, driverDemanderService.getInjector());
        driverDemanderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        AbstractDataSourceService dataSourceService = createDataSourceService(dsName, jndiName);

        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        final ServiceName dataSourceServiceNameAlias = AbstractDataSourceService.getServiceName(bindInfo);
        final ServiceName dataSourceServiceName = context.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY_NAME, dsName, DataSource.class);
        final ServiceBuilder<?> dataSourceServiceBuilder =
                Services.addServerExecutorDependency(
                        serviceTarget.addService(dataSourceServiceName, dataSourceService),
                        dataSourceService.getExecutorServiceInjector(), false)
                        .addAliases(dataSourceServiceNameAlias)
                .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                        dataSourceService.getManagementRepositoryInjector())
                .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                        dataSourceService.getSubjectFactoryInjector())
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        dataSourceService.getDriverRegistryInjector())
                .addDependency(SimpleSecurityManagerService.SERVICE_NAME, ServerSecurityManager.class, dataSourceService.getServerSecurityManager())
                .addDependency(ConnectorServices.IDLE_REMOVER_SERVICE)
                .addDependency(ConnectorServices.CONNECTION_VALIDATOR_SERVICE)
                .addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, dataSourceService.getMdrInjector())
                .addDependency(NamingService.SERVICE_NAME);
        if (jta) {
            dataSourceServiceBuilder.addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class, dataSourceService.getTransactionIntegrationInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, dataSourceService.getCcmInjector())
                    .addDependency(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(DEFAULT_NAME))
                    .addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class, dataSourceService.getRaRepositoryInjector());

        } else {
            dataSourceServiceBuilder.addDependency(ConnectorServices.NON_JTA_DS_RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class, dataSourceService.getRaRepositoryInjector())
                    .addDependency(ConnectorServices.NON_TX_CCM_SERVICE, CachedConnectionManager.class, dataSourceService.getCcmInjector());

        }
        //Register an empty override model regardless of we're enabled or not - the statistics listener will add the relevant childresources
        if (registration.isAllowsOverride()) {
            registration.registerOverrideModel(dsName, DataSourcesSubsystemProviders.OVERRIDE_DS_DESC);
        }
        startConfigAndAddDependency(dataSourceServiceBuilder, dataSourceService, dsName, serviceTarget, operation);

        dataSourceServiceBuilder.addDependency(driverServiceName, Driver.class,
                dataSourceService.getDriverInjector());

        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.NEVER);

        dataSourceServiceBuilder.install();
        driverDemanderBuilder.install();
    }


    static void secondRuntimeStep(OperationContext context, ModelNode operation, ManagementResourceRegistration datasourceRegistration, ModelNode model, boolean isXa) throws OperationFailedException {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ModelNode address = operation.require(OP_ADDR);
        final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
        final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final List<ServiceName> serviceNames = registry.getServiceNames();


        final boolean jta;
        if (isXa) {
            jta = true;
            final ModifiableXaDataSource dataSourceConfig;
            try {
                dataSourceConfig = xaFrom(context, model, dsName);
            } catch (ValidateException e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToCreate("XaDataSource", operation, e.getLocalizedMessage()));
            }
            final ServiceName xaDataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
            final XADataSourceConfigService xaDataSourceConfigService = new XADataSourceConfigService(dataSourceConfig);

            final ServiceBuilder<?> builder = serviceTarget.addService(xaDataSourceConfigServiceName, xaDataSourceConfigService);
            // add dependency on security domain service if applicable
            final DsSecurity dsSecurityConfig = dataSourceConfig.getSecurity();
            if (dsSecurityConfig != null) {
                final String securityDomainName = dsSecurityConfig.getSecurityDomain();
                if (securityDomainName != null) {
                    builder.addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomainName));
                }
            }
            // add dependency on security domain service if applicable for recovery config
            if (dataSourceConfig.getRecovery() != null) {
                final Credential credential = dataSourceConfig.getRecovery().getCredential();
                if (credential != null) {
                    final String securityDomainName = credential.getSecurityDomain();
                    if (securityDomainName != null) {
                        builder.addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomainName));
                    }
                }
            }
            int propertiesCount = 0;
            for (ServiceName name : serviceNames) {
                if (xaDataSourceConfigServiceName.append("xa-datasource-properties").isParentOf(name)) {
                    final ServiceController<?> xaConfigPropertyController = registry.getService(name);
                    XaDataSourcePropertiesService xaPropService = (XaDataSourcePropertiesService) xaConfigPropertyController.getService();

                    if (!ServiceController.State.UP.equals(xaConfigPropertyController.getState())) {
                        propertiesCount++;
                        xaConfigPropertyController.setMode(ServiceController.Mode.ACTIVE);
                        builder.addDependency(name, String.class, xaDataSourceConfigService.getXaDataSourcePropertyInjector(xaPropService.getName()));

                    } else {
                        throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.serviceAlreadyStarted("Data-source.xa-config-property", name));
                    }
                }
            }
            if (propertiesCount == 0) {
                throw ConnectorLogger.ROOT_LOGGER.xaDataSourcePropertiesNotPresent();
            }
            builder.install();

        } else {

            final ModifiableDataSource dataSourceConfig;
            try {
                dataSourceConfig = from(context, model, dsName);
            } catch (ValidateException e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToCreate("DataSource", operation, e.getLocalizedMessage()));
            }
            jta = dataSourceConfig.isJTA();
            final ServiceName dataSourceCongServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
            final DataSourceConfigService configService = new DataSourceConfigService(dataSourceConfig);

            final ServiceBuilder<?> builder = serviceTarget.addService(dataSourceCongServiceName, configService);
            // add dependency on security domain service if applicable
            final DsSecurity dsSecurityConfig = dataSourceConfig.getSecurity();
            if (dsSecurityConfig != null) {
                final String securityDomainName = dsSecurityConfig.getSecurityDomain();
                if (securityDomainName != null) {
                    builder.addDependency(SecurityDomainService.SERVICE_NAME.append(securityDomainName));
                }
            }
            for (ServiceName name : serviceNames) {
                if (dataSourceCongServiceName.append("connection-properties").isParentOf(name)) {
                    final ServiceController<?> dataSourceController = registry.getService(name);
                    ConnectionPropertiesService connPropService = (ConnectionPropertiesService) dataSourceController.getService();

                    if (!ServiceController.State.UP.equals(dataSourceController.getState())) {
                        dataSourceController.setMode(ServiceController.Mode.ACTIVE);
                        builder.addDependency(name, String.class, configService.getConnectionPropertyInjector(connPropService.getName()));

                    } else {
                        throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.serviceAlreadyStarted("Data-source.connectionProperty", name));
                    }
                }
            }
            builder.install();
        }

        final ServiceName dataSourceServiceName = context.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY_NAME, dsName, DataSource.class);
        final ServiceName dataSourceServiceNameAlias = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName).append(Constants.STATISTICS);


        final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);

        if (dataSourceController != null) {
            if (!ServiceController.State.UP.equals(dataSourceController.getState())) {
                final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
                DataSourceStatisticsService statsService = new DataSourceStatisticsService(datasourceRegistration, statsEnabled);
                serviceTarget.addService(dataSourceServiceName.append(Constants.STATISTICS), statsService)
                        .addAliases(dataSourceServiceNameAlias)
                        .addDependency(dataSourceServiceName)
                        .addDependency(CommonDeploymentService.getServiceName( ContextNames.bindInfoFor(jndiName)), CommonDeployment.class, statsService.getCommonDeploymentInjector())
                        .setInitialMode(ServiceController.Mode.PASSIVE)
                        .install();
                dataSourceController.setMode(ServiceController.Mode.ACTIVE);
            } else {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.serviceAlreadyStarted("Data-source", dsName));
            }
        } else {
            throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.serviceNotAvailable("Data-source", dsName));
        }

        final DataSourceReferenceFactoryService referenceFactoryService = new DataSourceReferenceFactoryService();
        final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                .append(dsName);
        final ServiceBuilder<?> referenceBuilder = serviceTarget.addService(referenceFactoryServiceName,
                referenceFactoryService).addDependency(dataSourceServiceName, DataSource.class,
                referenceFactoryService.getDataSourceInjector());

        referenceBuilder.install();

        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final BinderService binderService = new BinderService(bindInfo.getBindName());
        final ServiceBuilder<?> binderBuilder = serviceTarget
                .addService(bindInfo.getBinderServiceName(), binderService)
                .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class, binderService.getManagedObjectInjector())
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new AbstractServiceListener<Object>() {
                    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
                        switch (transition) {
                            case STARTING_to_UP: {
                                if (jta) {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundDataSource(jndiName);
                                } else {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundNonJTADataSource(jndiName);
                                }
                                break;
                            }
                            case STOPPING_to_DOWN: {
                                if (jta) {
                                    SUBSYSTEM_DATASOURCES_LOGGER.unboundDataSource(jndiName);
                                } else {
                                    SUBSYSTEM_DATASOURCES_LOGGER.unBoundNonJTADataSource(jndiName);
                                }
                                break;
                            }
                            case REMOVING_to_REMOVED: {
                                SUBSYSTEM_DATASOURCES_LOGGER.debugf("Removed JDBC Data-source [%s]", jndiName);
                                break;
                            }
                        }
                    }
                });
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        binderBuilder.install();

    }

    protected abstract void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder,
                                                        AbstractDataSourceService dataSourceService, String jndiName, ServiceTarget serviceTarget, final ModelNode operation)
            throws OperationFailedException;

    protected abstract AbstractDataSourceService createDataSourceService(final String dsName, final String jndiName) throws OperationFailedException;

    protected abstract boolean isXa();

    static Collection<AttributeDefinition> join(final AttributeDefinition[] a, final AttributeDefinition[] b) {
        final List<AttributeDefinition> result = new ArrayList<>();
        result.addAll(Arrays.asList(a));
        result.addAll(Arrays.asList(b));
        return result;
    }


}
