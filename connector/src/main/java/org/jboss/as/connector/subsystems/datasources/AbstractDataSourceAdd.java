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

import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCE_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDI_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.JTA;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS_ENABLED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.sql.Driver;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.connector.services.driver.registry.DriverRegistry;
import org.jboss.as.connector.util.ConnectorServices;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.security.service.SubjectFactoryService;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.core.api.connectionmanager.ccm.CachedConnectionManager;
import org.jboss.jca.core.api.management.ManagementRepository;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueInjectionService;
import org.jboss.security.SubjectFactory;

/**
 * Abstract operation handler responsible for adding a DataSource.
 *
 * @author John Bailey
 */
public abstract class AbstractDataSourceAdd extends AbstractAddStepHandler {


    /**
     * Overrides superclass method to pass the full {@code Resource} into the runtime handling logic.
     *
     * {@inheritDoc}
     */
    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final Resource resource = createResource(context);
        populateModel(context, operation, resource);
        final ModelNode model = resource.getModel();
        boolean enabled = ! operation.hasDefined(ENABLED.getName()) || operation.get(ENABLED.getName()).asBoolean();

        if (requiresRuntime(context)) {
            context.addStep(new OperationStepHandler() {
                public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
                    final List<ServiceController<?>> controllers = new ArrayList<ServiceController<?>>();
                    final ServiceVerificationHandler verificationHandler = new ServiceVerificationHandler();
                    performRuntime(context, operation, resource, model, verificationHandler, controllers);

                    if(requiresRuntimeVerification()) {
                        context.addStep(verificationHandler, OperationContext.Stage.VERIFY);
                    }

                    context.completeStep(new OperationContext.RollbackHandler() {
                        @Override
                        public void handleRollback(OperationContext context, ModelNode operation) {
                            rollbackRuntime(context, operation, model, controllers);
                        }
                    });
                }
            }, OperationContext.Stage.RUNTIME);
        }
        if (enabled) {
            context.addStep(new DataSourceEnable(this instanceof XaDataSourceAdd), OperationContext.Stage.MODEL);
        }
        context.stepCompleted();
    }

    /**
     * Method is {@code final}, and throws unsupported operation exception to prevent subclasses inadvertently
     * overridding it.
     *
     * {@inheritDoc}
     */
    @Override
    protected final void performRuntime(OperationContext context, ModelNode operation, ModelNode model, ServiceVerificationHandler verificationHandler, List<ServiceController<?>> newControllers) throws OperationFailedException {
        throw new UnsupportedOperationException();
    }

    private void performRuntime(final OperationContext context, final ModelNode operation, final Resource resource, final ModelNode model,
                                final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> controllers) throws OperationFailedException {
        final ModelNode address = operation.require(OP_ADDR);
        final String dsName = PathAddress.pathAddress(address).getLastElement().getValue();
        final String jndiName = model.get(JNDI_NAME.getName()).asString();
        boolean jta = JTA.resolveModelAttribute(context, operation).asBoolean();
        final boolean statsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();

        final ServiceTarget serviceTarget = context.getServiceTarget();


        ModelNode node = DATASOURCE_DRIVER.resolveModelAttribute(context, model);

        final String driverName = node.asString();
        final ServiceName driverServiceName = ServiceName.JBOSS.append("jdbc-driver", driverName.replaceAll("\\.", "_"));


        ValueInjectionService driverDemanderService = new ValueInjectionService<Driver>();

        final ServiceName driverDemanderServiceName = ServiceName.JBOSS.append("driver-demander").append(jndiName);
                final ServiceBuilder<?> driverDemanderBuilder = serviceTarget
                        .addService(driverDemanderServiceName, driverDemanderService)
                        .addDependency(driverServiceName, Driver.class,
                                driverDemanderService.getInjector());
        driverDemanderBuilder.addListener(verificationHandler);
        driverDemanderBuilder.setInitialMode(ServiceController.Mode.ACTIVE);

        AbstractDataSourceService dataSourceService = createDataSourceService(dsName);

        final ManagementResourceRegistration registration = context.getResourceRegistrationForUpdate();

        final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
        final ServiceBuilder<?> dataSourceServiceBuilder =
                Services.addServerExecutorDependency(
                        serviceTarget.addService(dataSourceServiceName, dataSourceService),
                        dataSourceService.getExecutorServiceInjector(), false)
                .addDependency(ConnectorServices.MANAGEMENT_REPOSITORY_SERVICE, ManagementRepository.class,
                        dataSourceService.getManagementRepositoryInjector())
                .addDependency(SubjectFactoryService.SERVICE_NAME, SubjectFactory.class,
                        dataSourceService.getSubjectFactoryInjector())
                .addDependency(ConnectorServices.JDBC_DRIVER_REGISTRY_SERVICE, DriverRegistry.class,
                        dataSourceService.getDriverRegistryInjector())
                .addDependency(ConnectorServices.IDLE_REMOVER_SERVICE)
                .addDependency(ConnectorServices.CONNECTION_VALIDATOR_SERVICE)
                .addDependency(NamingService.SERVICE_NAME);
        if (jta) {
            dataSourceServiceBuilder.addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class, dataSourceService.getTransactionIntegrationInjector())
                    .addDependency(ConnectorServices.CCM_SERVICE, CachedConnectionManager.class, dataSourceService.getCcmInjector());

        }
        //Register an empty override model regardless of we're enabled or not - the statistics listener will add the relevant childresources
        if (registration.isAllowsOverride()) {
            registration.registerOverrideModel(dsName, DataSourcesSubsystemProviders.OVERRIDE_DS_DESC);
        }
        dataSourceServiceBuilder.addListener(verificationHandler);
        startConfigAndAddDependency(dataSourceServiceBuilder, dataSourceService, dsName, serviceTarget, operation, verificationHandler);

        dataSourceServiceBuilder.addDependency(driverServiceName, Driver.class,
                    dataSourceService.getDriverInjector());

        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.NEVER);

        controllers.add(dataSourceServiceBuilder.install());
        controllers.add(driverDemanderBuilder.install());

    }

    protected abstract void startConfigAndAddDependency(ServiceBuilder<?> dataSourceServiceBuilder,
            AbstractDataSourceService dataSourceService, String jndiName, ServiceTarget serviceTarget, final ModelNode operation, final ServiceVerificationHandler serviceVerificationHandler)
            throws OperationFailedException;

    protected abstract void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException;

    protected abstract AbstractDataSourceService createDataSourceService(final String jndiName) throws OperationFailedException;

    static void populateAddModel(final ModelNode operation, final ModelNode modelNode,
            final String connectionPropertiesProp, final SimpleAttributeDefinition[] attributes, PropertiesAttributeDefinition[] properties) throws OperationFailedException {
        if (operation.hasDefined(connectionPropertiesProp)) {
            for (Property property : operation.get(connectionPropertiesProp).asPropertyList()) {
                modelNode.get(connectionPropertiesProp, property.getName()).set(property.getValue().asString());
            }
        }
        for (final SimpleAttributeDefinition attribute : attributes) {
            attribute.validateAndSet(operation, modelNode);
        }

        for (final PropertiesAttributeDefinition attribute : properties) {
            attribute.validateAndSet(operation, modelNode);
        }
    }

}
