/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.connector.subsystems.datasources;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_DATASOURCES_LOGGER;
import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.subsystems.common.jndi.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_AUTHENTICATION_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_ELYTRON_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.RECOVERY_SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.from;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.xaFrom;
import static org.jboss.as.connector.subsystems.jca.Constants.DEFAULT_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.security.CredentialReference.CREDENTIAL_REFERENCE;
import static org.jboss.as.controller.security.CredentialReference.handleCredentialReferenceUpdate;
import static org.jboss.as.controller.security.CredentialReference.rollbackCredentialStoreUpdate;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.sql.DataSource;

import org.jboss.as.connector._private.Capabilities;
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
import org.jboss.as.controller.capability.CapabilityServiceSupport;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingService;
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
import org.jboss.msc.Service;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.common.function.ExceptionSupplier;
import org.wildfly.security.auth.client.AuthenticationContext;
import org.wildfly.security.credential.source.CredentialSource;

/**
 * Abstract operation handler responsible for adding a DataSource.
 *
 * @author John Bailey
 */
public abstract class AbstractDataSourceAdd extends AbstractAddStepHandler {

    private static final ServiceName SECURITY_DOMAIN_SERVICE = ServiceName.JBOSS.append("security", "security-domain");
    private static final ServiceName SECURITY_MANAGER_SERVICE = ServiceName.JBOSS.append("security", "simple-security-manager");
    private static final ServiceName SUBJECT_FACTORY_SERVICE = ServiceName.JBOSS.append("security", "subject-factory");

    AbstractDataSourceAdd(Collection<AttributeDefinition> attributes) {
        super(attributes);
    }


    @Override
    protected void populateModel(final OperationContext context, final ModelNode operation, final Resource resource) throws OperationFailedException {
        if (context.getProcessType().isServer()) {
            DataSourceStatisticsService.registerStatisticsResources(resource);
        }
        super.populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();
        handleCredentialReferenceUpdate(context, model.get(CREDENTIAL_REFERENCE), CREDENTIAL_REFERENCE);
        handleCredentialReferenceUpdate(context, model.get(Constants.RECOVERY_CREDENTIAL_REFERENCE.getName()), Constants.RECOVERY_CREDENTIAL_REFERENCE.getName());
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

    @Override
    protected void rollbackRuntime(OperationContext context, final ModelNode operation, final Resource resource) {
        rollbackCredentialStoreUpdate(Constants.CREDENTIAL_REFERENCE, context, resource);
        rollbackCredentialStoreUpdate(Constants.RECOVERY_CREDENTIAL_REFERENCE, context, resource);
    }

     void firstRuntimeStep(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();
        final ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor(jndiName);
        final boolean jta = JTA.resolveModelAttribute(context, operation).asBoolean();
        final String dsName = context.getCurrentAddressValue();// The STATISTICS_ENABLED.resolveModelAttribute(context, model) call should remain as it serves to validate that any
        // expression in the model can be resolved to a correct value.
        @SuppressWarnings("unused")
        final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        final CapabilityServiceSupport support = context.getCapabilityServiceSupport();
        final ServiceTarget serviceTarget = context.getServiceTarget();
        final ModelNode node = DATASOURCE_DRIVER.resolveModelAttribute(context, model);
        final String driverName = node.asString();
        final ServiceName driverServiceName = ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_"));

        final ServiceName driverDemanderServiceName = ServiceName.JBOSS.append("driver-demander").append(jndiName);
        final ServiceBuilder<?> driverDemanderBuilder = serviceTarget.addService(driverDemanderServiceName);
        final Consumer<Driver> driverConsumer = driverDemanderBuilder.provides(driverDemanderServiceName);
        final Supplier<Driver> driverSupplier = driverDemanderBuilder.requires(driverServiceName);
        driverDemanderBuilder.setInstance(new DriverDemanderService(driverConsumer, driverSupplier));
        driverDemanderBuilder.install();

        AbstractDataSourceService dataSourceService = createDataSourceService(dsName, jndiName);

        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
        final ServiceName dataSourceServiceNameAlias = AbstractDataSourceService.getServiceName(bindInfo);
        final ServiceName dataSourceServiceName = context.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY_NAME, dsName, DataSource.class);
        final ServiceBuilder<?> dataSourceServiceBuilder =
                Services.addServerExecutorDependency(
                        serviceTarget.addService(dataSourceServiceName, dataSourceService),
                        dataSourceService.getExecutorServiceInjector())
                        .addAliases(dataSourceServiceNameAlias)
                .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                        dataSourceService.getManagementRepositoryInjector())
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        dataSourceService.getDriverRegistryInjector());
         dataSourceServiceBuilder.requires(ConnectorServices.IDLE_REMOVER_SERVICE);
         dataSourceServiceBuilder.requires(ConnectorServices.CONNECTION_VALIDATOR_SERVICE);
         dataSourceServiceBuilder.addDependency(ConnectorServices.IRONJACAMAR_MDR, MetadataRepository.class, dataSourceService.getMdrInjector());
         dataSourceServiceBuilder.requires(support.getCapabilityServiceName(NamingService.CAPABILITY_NAME));

        if (jta) {
            dataSourceServiceBuilder.addDependency(support.getCapabilityServiceName(ConnectorServices.TRANSACTION_INTEGRATION_CAPABILITY_NAME), TransactionIntegration.class, dataSourceService.getTransactionIntegrationInjector());
            dataSourceServiceBuilder.addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, dataSourceService.getCcmInjector());
            dataSourceServiceBuilder.requires(ConnectorServices.BOOTSTRAP_CONTEXT_SERVICE.append(DEFAULT_NAME));
            dataSourceServiceBuilder.addDependency(ConnectorServices.RA_REPOSITORY_SERVICE, ResourceAdapterRepository.class, dataSourceService.getRaRepositoryInjector());

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

         // If the authentication context is defined, add the capability
         boolean requireLegacySecurity = false;
        if (ELYTRON_ENABLED.resolveModelAttribute(context, model).asBoolean()) {
            if (model.hasDefined(AUTHENTICATION_CONTEXT.getName())) {
                dataSourceServiceBuilder.addDependency(
                        context.getCapabilityServiceName(
                                Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY,
                                AUTHENTICATION_CONTEXT.resolveModelAttribute(context, model).asString(),
                                AuthenticationContext.class),
                        AuthenticationContext.class,
                        dataSourceService.getAuthenticationContext()
                );
            }
        } else {
            String secDomain = SECURITY_DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
            requireLegacySecurity = (secDomain != null && secDomain.length() > 0) ;
        }

         if (isXa()) {
             if (RECOVERY_ELYTRON_ENABLED.resolveModelAttribute(context, model).asBoolean()) {
                 if (model.hasDefined(RECOVERY_AUTHENTICATION_CONTEXT.getName())) {
                     dataSourceServiceBuilder.addDependency(
                             context.getCapabilityServiceName(
                                     Capabilities.AUTHENTICATION_CONTEXT_CAPABILITY,
                                     RECOVERY_AUTHENTICATION_CONTEXT.resolveModelAttribute(context, model).asString(),
                                     AuthenticationContext.class),
                             AuthenticationContext.class,
                             dataSourceService.getRecoveryAuthenticationContext()
                     );
                 }
             } else if (!requireLegacySecurity) {
                 String secDomain = RECOVERY_SECURITY_DOMAIN.resolveModelAttribute(context, model).asStringOrNull();
                 requireLegacySecurity = (secDomain != null && secDomain.length() > 0);
             }
         }

         if (requireLegacySecurity) {
             context.setRollbackOnly();
             throw SUBSYSTEM_RA_LOGGER.legacySecurityNotAvailable(dsName);
         }

         ModelNode credentialReference = Constants.CREDENTIAL_REFERENCE.resolveModelAttribute(context, model);
         if (credentialReference.isDefined()) {
             dataSourceService.getCredentialSourceSupplierInjector()
                     .inject(
                             CredentialReference.getCredentialSourceSupplier(context, Constants.CREDENTIAL_REFERENCE, model, dataSourceServiceBuilder));
         }

         ModelNode recoveryCredentialReference = Constants.RECOVERY_CREDENTIAL_REFERENCE.resolveModelAttribute(context, model);
         if (recoveryCredentialReference.isDefined()) {
             dataSourceService.getRecoveryCredentialSourceSupplierInjector()
                     .inject(
                             CredentialReference.getCredentialSourceSupplier(context, Constants.RECOVERY_CREDENTIAL_REFERENCE, model, dataSourceServiceBuilder));
         }

        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.NEVER);
        dataSourceServiceBuilder.install();
    }


    static void secondRuntimeStep(OperationContext context, ModelNode operation, ManagementResourceRegistration datasourceRegistration, ModelNode model, boolean isXa) throws OperationFailedException {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ModelNode address = operation.require(OP_ADDR);
        final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
        final String jndiName = JNDI_NAME.resolveModelAttribute(context, model).asString();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final List<ServiceName> serviceNames = registry.getServiceNames();

        final boolean elytronEnabled = ELYTRON_ENABLED.resolveModelAttribute(context, model).asBoolean();

        final ServiceName dataSourceServiceName = context.getCapabilityServiceName(Capabilities.DATA_SOURCE_CAPABILITY_NAME, dsName, DataSource.class);
        final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);

        final ExceptionSupplier<CredentialSource, Exception> credentialSourceExceptionExceptionSupplier =
                dataSourceController.getService() instanceof AbstractDataSourceService ?
                        ((AbstractDataSourceService)dataSourceController.getService()).getCredentialSourceSupplierInjector().getOptionalValue()
                        :
                        null;
        final ExceptionSupplier<CredentialSource, Exception> recoveryCredentialSourceExceptionExceptionSupplier =
                dataSourceController.getService() instanceof AbstractDataSourceService ?
                        ((AbstractDataSourceService)dataSourceController.getService()).getRecoveryCredentialSourceSupplierInjector().getOptionalValue()
                        :
                        null;

        final boolean jta;
        if (isXa) {
            jta = true;
            final ModifiableXaDataSource dataSourceConfig;
            try {
                dataSourceConfig = xaFrom(context, model, dsName, credentialSourceExceptionExceptionSupplier, recoveryCredentialSourceExceptionExceptionSupplier);
            } catch (ValidateException e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToCreate("XaDataSource", operation, e.getLocalizedMessage()));
            }
            final ServiceName xaDataSourceConfigServiceName = XADataSourceConfigService.SERVICE_NAME_BASE.append(dsName);
            final ServiceBuilder<?> builder = serviceTarget.addService(xaDataSourceConfigServiceName);
            // add dependency on security domain service if applicable
            final DsSecurity dsSecurityConfig = dataSourceConfig.getSecurity();
            if (dsSecurityConfig != null) {
                final String securityDomainName = dsSecurityConfig.getSecurityDomain();
                if (!elytronEnabled && securityDomainName != null) {
                    builder.requires(SECURITY_DOMAIN_SERVICE.append(securityDomainName));
                }
            }
            // add dependency on security domain service if applicable for recovery config
            if (dataSourceConfig.getRecovery() != null) {
                final Credential credential = dataSourceConfig.getRecovery().getCredential();
                if (credential != null) {
                    final String securityDomainName = credential.getSecurityDomain();
                    if (!RECOVERY_ELYTRON_ENABLED.resolveModelAttribute(context, model).asBoolean() && securityDomainName != null) {
                        builder.requires(SECURITY_DOMAIN_SERVICE.append(securityDomainName));
                    }
                }
            }
            int propertiesCount = 0;
            final Map<String, Supplier<String>> xaDataSourceProperties = new HashMap<>();
            for (ServiceName name : serviceNames) {
                if (xaDataSourceConfigServiceName.append("xa-datasource-properties").isParentOf(name)) {
                    final ServiceController<?> xaConfigPropertyController = registry.getService(name);
                    XaDataSourcePropertiesService xaPropService = (XaDataSourcePropertiesService) xaConfigPropertyController.getService();

                    if (!ServiceController.State.UP.equals(xaConfigPropertyController.getState())) {
                        propertiesCount++;
                        xaConfigPropertyController.setMode(ServiceController.Mode.ACTIVE);
                        xaDataSourceProperties.put(xaPropService.getName(), builder.requires(name));

                    } else {
                        throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.serviceAlreadyStarted("Data-source.xa-config-property", name));
                    }
                }
            }
            if (propertiesCount == 0) {
                throw ConnectorLogger.ROOT_LOGGER.xaDataSourcePropertiesNotPresent();
            }
            final XADataSourceConfigService xaDataSourceConfigService = new XADataSourceConfigService(dataSourceConfig, xaDataSourceProperties);
            builder.setInstance(xaDataSourceConfigService);
            builder.install();

        } else {

            final ModifiableDataSource dataSourceConfig;
            try {
                dataSourceConfig = from(context, model, dsName, credentialSourceExceptionExceptionSupplier);
            } catch (ValidateException e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToCreate("DataSource", operation, e.getLocalizedMessage()));
            }
            jta = dataSourceConfig.isJTA();
            final ServiceName dataSourceCongServiceName = DataSourceConfigService.SERVICE_NAME_BASE.append(dsName);

            final ServiceBuilder<?> builder = serviceTarget.addService(dataSourceCongServiceName);
            // add dependency on security domain service if applicable
            final DsSecurity dsSecurityConfig = dataSourceConfig.getSecurity();
            if (dsSecurityConfig != null) {
                final String securityDomainName = dsSecurityConfig.getSecurityDomain();
                if (!elytronEnabled && securityDomainName != null) {
                    builder.requires(SECURITY_DOMAIN_SERVICE.append(securityDomainName));
                }
            }
            final Map<String, Supplier<String>> connectionProperties = new HashMap<>();
            for (ServiceName name : serviceNames) {
                if (dataSourceCongServiceName.append("connection-properties").isParentOf(name)) {
                    final ServiceController<?> connPropServiceController = registry.getService(name);
                    ConnectionPropertiesService connPropService = (ConnectionPropertiesService) connPropServiceController.getService();

                    if (!ServiceController.State.UP.equals(connPropServiceController.getState())) {
                        connPropServiceController.setMode(ServiceController.Mode.ACTIVE);
                        connectionProperties.put(connPropService.getName(), builder.requires(name));

                    } else {
                        throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.serviceAlreadyStarted("Data-source.connectionProperty", name));
                    }
                }
            }
            final DataSourceConfigService configService = new DataSourceConfigService(dataSourceConfig, connectionProperties);
            builder.setInstance(configService);
            builder.install();
        }

        final ServiceName dataSourceServiceNameAlias = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName).append(Constants.STATISTICS);

        if (dataSourceController != null) {
            if (!ServiceController.State.UP.equals(dataSourceController.getState())) {
                final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
                DataSourceStatisticsService statsService = new DataSourceStatisticsService(datasourceRegistration, statsEnabled);
                final ServiceBuilder statsServiceSB = serviceTarget.addService(dataSourceServiceName.append(Constants.STATISTICS), statsService);
                statsServiceSB.addAliases(dataSourceServiceNameAlias);
                statsServiceSB.requires(dataSourceServiceName);
                statsServiceSB.addDependency(CommonDeploymentService.getServiceName( ContextNames.bindInfoFor(jndiName)), CommonDeployment.class, statsService.getCommonDeploymentInjector());
                statsServiceSB.setInitialMode(ServiceController.Mode.PASSIVE);
                statsServiceSB.install();
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
                .addDependency(bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector()).addListener(new LifecycleListener() {
                    private volatile boolean bound;
                    public void handleEvent(final ServiceController<? extends Object> controller, final LifecycleEvent event) {
                        switch (event) {
                            case UP: {
                                if (jta) {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundDataSource(jndiName);
                                } else {
                                    SUBSYSTEM_DATASOURCES_LOGGER.boundNonJTADataSource(jndiName);
                                }
                                bound = true;
                                break;
                            }
                            case DOWN: {
                                if (bound) {
                                    if (jta) {
                                        SUBSYSTEM_DATASOURCES_LOGGER.unboundDataSource(jndiName);
                                    } else {
                                        SUBSYSTEM_DATASOURCES_LOGGER.unBoundNonJTADataSource(jndiName);
                                    }
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

    private static final class DriverDemanderService implements Service {
        private final Consumer<Driver> driverConsumer;
        private final Supplier<Driver> driverSupplier;
        private DriverDemanderService(final Consumer<Driver> driverConsumer, final Supplier<Driver> driverSupplier) {
            this.driverConsumer = driverConsumer;
            this.driverSupplier = driverSupplier;
        }

        @Override
        public void start(final StartContext startContext) {
            driverConsumer.accept(driverSupplier.get());
        }

        @Override
        public void stop(final StopContext stopContext) {
            driverConsumer.accept(null);
        }
    }
}
