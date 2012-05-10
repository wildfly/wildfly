/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.management.connections.database;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import java.util.List;

import javax.sql.DataSource;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
/**
 * Handler for adding database management connections.
 *
 * @author <a href="mailto:flemming.harms@gmail.com">Flemming Harms</a>
 */
public class DatabaseConnectionAddHandler extends AbstractAddStepHandler {

    public static final DatabaseConnectionAddHandler INSTANCE = new DatabaseConnectionAddHandler();

    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : DatabaseConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            attr.validateAndSet(operation, model);
        }
    }

    @Override
    protected boolean requiresRuntime(OperationContext context) {
        return true;
    }

    protected void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                  final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        PathAddress address = PathAddress.pathAddress(operation.get(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ModelNode resolvedModel = createResolvedDatabaseConfiguration(context, model);

        final ServiceTarget serviceTarget = context.getServiceTarget();
        final DatabaseConnectionManagerService connectionManagerService = new DatabaseConnectionManagerService(resolvedModel);

        ServiceBuilder<DatabaseConnectionManagerService> serviceBuilder = serviceTarget.addService(DatabaseConnectionManagerService.BASE_SERVICE_NAME.append(name), connectionManagerService)
        .setInitialMode(ServiceController.Mode.ON_DEMAND);

        if (resolvedModel.hasDefined(DatabaseConnectionResourceDefinition.DATA_SOURCE.getName())) {
            ServiceName datasourceService = ServiceName.JBOSS.append("data-source").append(resolvedModel.get(DatabaseConnectionResourceDefinition.DATA_SOURCE.getName()).asString());
            serviceBuilder.addDependency(datasourceService,DataSource.class,connectionManagerService.getDatasource());
        }

        ServiceController<DatabaseConnectionManagerService> sc = serviceBuilder.install();
        if (newControllers != null) {
            newControllers.add(sc);
        }
    }

    static ModelNode createResolvedDatabaseConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        final ModelNode resolvedModel = new ModelNode();
        for (AttributeDefinition attr : DatabaseConnectionResourceDefinition.ATTRIBUTE_DEFINITIONS) {
            resolvedModel.get(attr.getName()).set(attr.resolveModelAttribute(context, model));
        }
        return resolvedModel;
    }


}
