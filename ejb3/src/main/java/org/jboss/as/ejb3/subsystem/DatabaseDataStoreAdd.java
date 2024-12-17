/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.Timer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.ejb3.timerservice.persistence.database.DatabaseTimerPersistence;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.wildfly.security.manager.WildFlySecurityManager;

import javax.sql.DataSource;

/**
 * Adds the timer service file based data store
 *
 * @author Stuart Douglas
 */
public class DatabaseDataStoreAdd extends AbstractAddStepHandler {

    private static final String TIMER_SERVICE_CAPABILITY_NAME = "org.wildfly.ejb3.timer-service";

    DatabaseDataStoreAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model) throws OperationFailedException {

        final String dsName = model.hasDefined(DatabaseDataStoreResourceDefinition.DATASOURCE_NAME.getName()) ?
                DatabaseDataStoreResourceDefinition.DATASOURCE_NAME.resolveModelAttribute(context, model).asString() : null;
        final String jndiName = model.hasDefined(DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.getName()) ?
                DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.resolveModelAttribute(context, model).asString() : null;

        final ModelNode dataBaseValue = DatabaseDataStoreResourceDefinition.DATABASE.resolveModelAttribute(context, model);
        final String database;
        if(dataBaseValue.isDefined()) {
            database = dataBaseValue.asString();
        } else {
            database = null;
        }
        final String partition = DatabaseDataStoreResourceDefinition.PARTITION.resolveModelAttribute(context, model).asString();

        int refreshInterval = DatabaseDataStoreResourceDefinition.REFRESH_INTERVAL.resolveModelAttribute(context, model).asInt();
        boolean allowExecution = DatabaseDataStoreResourceDefinition.ALLOW_EXECUTION.resolveModelAttribute(context, model).asBoolean();

        final String nodeName = WildFlySecurityManager.getPropertyPrivileged(ServerEnvironment.NODE_NAME, null);

        // add the TimerPersistence instance
        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<?> builder = serviceTarget.addCapability(TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY);
        final Consumer<DatabaseTimerPersistence> consumer = builder.provides(TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY);
        final Supplier<DataSource> dataSourceSupplier = dsName != null ? builder.requires(context.getCapabilityServiceName(DatabaseDataStoreResourceDefinition.DATA_SOURCE_CAPABILITY_NAME, dsName, DataSource.class)) : null;
        final Supplier<ManagedReferenceFactory> dataSourceReferenceSupplier = jndiName != null ? builder.requires(ContextNames.bindInfoFor(jndiName).getBinderServiceName()) : null;
        final Supplier<ModuleLoader> moduleLoaderSupplier = builder.requires(Services.JBOSS_SERVICE_MODULE_LOADER);
        final Supplier<Timer> timerSupplier = builder.requiresCapability(TIMER_SERVICE_CAPABILITY_NAME, java.util.Timer.class);
        final DatabaseTimerPersistence databaseTimerPersistence = new DatabaseTimerPersistence(consumer, dataSourceSupplier, dataSourceReferenceSupplier, moduleLoaderSupplier, timerSupplier, database, partition, nodeName, refreshInterval, allowExecution);
        builder.setInstance(databaseTimerPersistence);
        builder.install();
    }

}
