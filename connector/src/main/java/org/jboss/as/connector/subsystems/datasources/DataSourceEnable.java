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
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.from;
import static org.jboss.as.connector.subsystems.datasources.DataSourceModelNodeUtil.xaFrom;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import org.jboss.as.connector.logging.ConnectorLogger;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.security.service.SecurityDomainService;
import org.jboss.dmr.ModelNode;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.validator.ValidateException;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;

/**
 * Operation handler responsible for enabling an existing data-source.
 *
 * @author John Bailey
 */
public class DataSourceEnable implements OperationStepHandler {
    static final DataSourceEnable LOCAL_INSTANCE = new DataSourceEnable(false);
    static final DataSourceEnable XA_INSTANCE = new DataSourceEnable(true);

    private final boolean xa;

    public DataSourceEnable(boolean xa) {
        super();
        this.xa = xa;
    }

    public void execute(OperationContext context, ModelNode operation) {

        final Resource resource = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS);
        final ModelNode model = resource.getModel();
        if (! context.isBooting())
            model.get(ENABLED).set(true);

        if (context.isNormalServer()) {
            DataSourceStatisticsListener.registerStatisticsResources(resource);

            context.addStep(new OperationStepHandler() {
                public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
                    final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();
                    addServices(context, operation, registration, model, isXa());
                    context.completeStep(new OperationContext.RollbackHandler() {
                                            @Override
                                            public void handleRollback(OperationContext context, ModelNode operation) {
                                            }
                                        });
                }
            }, OperationContext.Stage.RUNTIME);
        }
        context.stepCompleted();
    }

    static void addServices(OperationContext context, ModelNode operation, ManagementResourceRegistration datasourceRegistration, ModelNode model, boolean isXa) throws OperationFailedException {
        final ServiceTarget serviceTarget = context.getServiceTarget();

        final ModelNode address = operation.require(OP_ADDR);
        final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
        final String jndiName = model.get(JNDI_NAME.getName()).asString();
        final ServiceRegistry registry = context.getServiceRegistry(true);
        final List<ServiceName> serviceNames = registry.getServiceNames();


        if (isXa) {
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
                dataSourceConfig = from(context, model,dsName);
            } catch (ValidateException e) {
                throw new OperationFailedException(ConnectorLogger.ROOT_LOGGER.failedToCreate("DataSource", operation, e.getLocalizedMessage()));
            }
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

        final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);


        final ServiceController<?> dataSourceController = registry.getService(dataSourceServiceName);

        if (dataSourceController != null) {
            if (!ServiceController.State.UP.equals(dataSourceController.getState())) {
                final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
                dataSourceController.addListener(new DataSourceStatisticsListener(datasourceRegistration, statsEnabled));

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
                referenceFactoryService).addDependency(dataSourceServiceName, javax.sql.DataSource.class,
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
                                SUBSYSTEM_DATASOURCES_LOGGER.boundDataSource(jndiName);
                                break;
                            }
                            case STOPPING_to_DOWN: {
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
        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);
        binderBuilder.install();

    }

    public static DataSourceEnable getLocalInstance() {
        return LOCAL_INSTANCE;
    }

    public boolean isXa() {
        return xa;
    }

}
