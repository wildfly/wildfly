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

import java.util.List;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.ejb3.deployment.processors.TimerServiceDeploymentProcessor;
import org.jboss.as.ejb3.timerservice.persistence.TimerPersistence;
import org.jboss.as.ejb3.timerservice.persistence.database.DatabaseTimerPersistence;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.server.Services;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleLoader;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

/**
 * Adds the timer service file based data store
 *
 * @author Stuart Douglas
 */
public class DatabaseDataStoreAdd extends AbstractAddStepHandler {

    public static final DatabaseDataStoreAdd INSTANCE = new DatabaseDataStoreAdd();

    protected void populateModel(ModelNode operation, ModelNode timerServiceModel) throws OperationFailedException {

        for (AttributeDefinition attr : DatabaseDataStoreResourceDefinition.ATTRIBUTES.values()) {
            attr.validateAndSet(operation, timerServiceModel);
        }
    }

    @Override
    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {

        final ServiceController<DatabaseTimerPersistence> controller = installRuntimeServices(context, operation, model, verificationHandler);

        if (newControllers != null) {
            newControllers.add(controller);
        }
    }

    public ServiceController<DatabaseTimerPersistence> installRuntimeServices(final OperationContext context, final ModelNode operation, final ModelNode model, final ServiceVerificationHandler verificationHandler) throws OperationFailedException {
        final String jndiName = DatabaseDataStoreResourceDefinition.DATASOURCE_JNDI_NAME.resolveModelAttribute(context, model).asString();

        final ModelNode dataBaseValue = DatabaseDataStoreResourceDefinition.DATABASE.resolveModelAttribute(context, model);
        final String database;
        if(dataBaseValue.isDefined()) {
            database = dataBaseValue.asString();
        } else {
            database = null;
        }
        final String partition = DatabaseDataStoreResourceDefinition.PARTITION.resolveModelAttribute(context, model).asString();

        final String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();

        int refreshInterval = DatabaseDataStoreResourceDefinition.REFRESH_INTERVAL.resolveModelAttribute(context, model).asInt();
        boolean allowExecution = DatabaseDataStoreResourceDefinition.ALLOW_EXECUTION.resolveModelAttribute(context, model).asBoolean();

        final DatabaseTimerPersistence databaseTimerPersistence = new DatabaseTimerPersistence(database, partition, refreshInterval, allowExecution);
        final ServiceName serviceName = TimerPersistence.SERVICE_NAME.append(name);
        final ServiceBuilder<DatabaseTimerPersistence> builder = context.getServiceTarget().addService(serviceName, databaseTimerPersistence);

        if (verificationHandler != null) {
            builder.addListener(verificationHandler);
        }

        return builder
                .addDependency(Services.JBOSS_SERVICE_MODULE_LOADER, ModuleLoader.class, databaseTimerPersistence.getModuleLoader())
                .addDependency(ContextNames.bindInfoFor(jndiName).getBinderServiceName(), ManagedReferenceFactory.class, databaseTimerPersistence.getDataSourceInjectedValue())
                .addDependency(TimerServiceDeploymentProcessor.TIMER_SERVICE_NAME, java.util.Timer.class, databaseTimerPersistence.getTimerInjectedValue())
                .install();
    }

}
