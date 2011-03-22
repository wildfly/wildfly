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

import java.sql.Driver;
import javax.sql.DataSource;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCEPROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.DataSourcesSubsystemProviders.XA_DATASOURCE_ATTRIBUTE;

import org.jboss.as.connector.ConnectorServices;
import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.NamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.as.naming.service.NamingService;
import org.jboss.as.txn.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.jca.core.spi.transaction.TransactionIntegration;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.util.Strings;

/**
 * Abstract operation handler responsible for adding a DataSource.
 * @author John Bailey
 */
public abstract class AbstractDataSourceAdd implements ModelAddOperationHandler {

    public static final Logger log = Logger.getLogger("org.jboss.as.connector.subsystems.datasources");

    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler)
            throws OperationFailedException {
        final ModelNode subModel = context.getSubModel();

        populateModel(operation, subModel);

        // Compensating is remove
        final ModelNode address = operation.require(OP_ADDR);
        final ModelNode compensating = Util.getResourceRemoveOperation(address);

        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceTarget serviceTarget = context.getServiceTarget();

                    boolean enabled = !operation.hasDefined(ENABLED) || operation.get(ENABLED).asBoolean();

                    final String rawJndiName = operation.require(JNDINAME).asString();
                    final String jndiName;
                    if (!rawJndiName.startsWith("java:/") && operation.hasDefined(USE_JAVA_CONTEXT)
                            && operation.get(USE_JAVA_CONTEXT).asBoolean()) {
                        jndiName = "java:/" + rawJndiName;
                    } else {
                        jndiName = rawJndiName;
                    }
                    final AbstractDataSourceService dataSourceService = createDataSourceService(jndiName, operation);

                    final ServiceName dataSourceServiceName = AbstractDataSourceService.SERVICE_NAME_BASE.append(jndiName);
                    final ServiceBuilder<?> dataSourceServiceBuilder = serviceTarget
                            .addService(dataSourceServiceName, dataSourceService)
                            .addDependency(ConnectorServices.TRANSACTION_INTEGRATION_SERVICE, TransactionIntegration.class,
                                    dataSourceService.getTransactionIntegrationInjector())
                            .addDependency(NamingService.SERVICE_NAME);

                    final String driverName = operation.require(DRIVER).asString();
                    final ServiceName driverServiceName = getDriverDependency(driverName);
                    if (driverServiceName != null) {
                        dataSourceServiceBuilder.addDependency(driverServiceName, Driver.class,
                                dataSourceService.getDriverInjector());
                    }

                    final DataSourceReferenceFactoryService referenceFactoryService = new DataSourceReferenceFactoryService();
                    final ServiceName referenceFactoryServiceName = DataSourceReferenceFactoryService.SERVICE_NAME_BASE
                            .append(jndiName);
                    final ServiceBuilder<?> referenceBuilder = serviceTarget.addService(referenceFactoryServiceName,
                            referenceFactoryService).addDependency(dataSourceServiceName, DataSource.class,
                            referenceFactoryService.getDataSourceInjector());

                    final BinderService binderService = new BinderService(jndiName.substring(6));
                    final ServiceName binderServiceName = ContextNames.JAVA_CONTEXT_SERVICE_NAME.append(jndiName);
                    final ServiceBuilder<?> binderBuilder = serviceTarget
                            .addService(binderServiceName, binderService)
                            .addDependency(referenceFactoryServiceName, ManagedReferenceFactory.class,
                                    binderService.getManagedObjectInjector())
                            .addDependency(ContextNames.JAVA_CONTEXT_SERVICE_NAME, NamingStore.class,
                                    binderService.getNamingStoreInjector()).addListener(new AbstractServiceListener<Object>() {
                                public void serviceStarted(ServiceController<?> controller) {
                                    log.infof("Bound JDBC Data-source [%s]", jndiName);
                                }

                                public void serviceStopped(ServiceController<?> serviceController) {
                                    log.infof("Unbound JDBC Data-source [%s]", jndiName);
                                }

                                public void serviceRemoved(ServiceController<?> serviceController) {
                                    log.infof("Removed JDBC Data-source [%s]", jndiName);
                                    serviceController.removeListener(this);
                                }
                            });

                    if (enabled) {
                        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
                        referenceBuilder.setInitialMode(ServiceController.Mode.ACTIVE).install();
                        binderBuilder.setInitialMode(ServiceController.Mode.ACTIVE)
                                .addListener(new ResultHandler.ServiceStartListener(resultHandler)).install();
                    } else {
                        dataSourceServiceBuilder.setInitialMode(ServiceController.Mode.NEVER).install();
                        referenceBuilder.setInitialMode(ServiceController.Mode.NEVER).install();
                        binderBuilder.setInitialMode(ServiceController.Mode.NEVER).install();
                        resultHandler.handleResultComplete();
                    }
                }
            });
        } else {
            resultHandler.handleResultComplete();
        }
        return new BasicOperationResult(compensating);
    }

    protected abstract void populateModel(final ModelNode operation, final ModelNode model);

    protected abstract AbstractDataSourceService createDataSourceService(final String jndiName, final ModelNode operation)
            throws OperationFailedException;

    private ServiceName getDriverDependency(final String driver) {
        String[] strings = Strings.split(driver, "#");
        if (strings.length != 2) {
            throw new IllegalArgumentException(
                    "module should define jdbc driver with this format: <driver-name>#<major-version>.<minor-version>");
        }
        final String driverName = strings[0];
        strings = Strings.split(strings[1], ".", 2);
        if (strings.length != 2) {
            throw new IllegalArgumentException(
                    "module should define jdbc driver with this format: <driver-name>#<major-version>.<minor-version>");
        }
        final Integer majorVersion;
        final Integer minorVersion;
        try {
            majorVersion = Integer.valueOf(strings[0]);
            minorVersion = Integer.valueOf(strings[1]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(
                    "module should define jdbc driver with this format: <driver-name>#<major-version>.<minor-version> "
                            + "version number should be valid Integer");
        }

        if (driverName != null & majorVersion != null && minorVersion != null) {
            return ServiceName.JBOSS.append("jdbc-driver", driverName, Integer.toString(majorVersion),
                    Integer.toString(minorVersion));
        }
        return null;
    }

    static void populateAddModel(final ModelNode existingModel, final ModelNode newModel,
            final String connectionPropertiesProp, final AttributeDefinition[] attributes) {
        if (existingModel.has(connectionPropertiesProp)) {
            for (Property property : existingModel.get(connectionPropertiesProp).asPropertyList()) {
                newModel.get(connectionPropertiesProp, property.getName()).set(property.getValue().asString());
            }
        }
        for (final AttributeDefinition attribute : attributes) {
            if (existingModel.get(attribute.getName()).isDefined()) {
                newModel.get(attribute.getName()).set(existingModel.get(attribute.getName()));
            }
        }

    }

}
