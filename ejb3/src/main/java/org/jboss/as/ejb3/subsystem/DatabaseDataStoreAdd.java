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

package org.jboss.as.ejb3.subsystem;

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

        final String jndiName = DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.resolveModelAttribute(context, model).asString();

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
        final DatabaseTimerPersistence databaseTimerPersistence = new DatabaseTimerPersistence(database, partition, nodeName, refreshInterval, allowExecution);

        // add the TimerPersistence instance
        final CapabilityServiceTarget serviceTarget = context.getCapabilityServiceTarget();
        final CapabilityServiceBuilder<DatabaseTimerPersistence> builder = serviceTarget.addCapability(TimerServiceResourceDefinition.TIMER_PERSISTENCE_CAPABILITY, databaseTimerPersistence);
        builder.addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, databaseTimerPersistence.getModuleLoader());
        builder.addDependency(ContextNames.bindInfoFor(jndiName).getBinderServiceName(), ManagedReferenceFactory.class, databaseTimerPersistence.getDataSourceInjectedValue());
        builder.addCapabilityRequirement(TIMER_SERVICE_CAPABILITY_NAME, java.util.Timer.class, databaseTimerPersistence.getTimerInjectedValue());
        builder.install();
    }

}
