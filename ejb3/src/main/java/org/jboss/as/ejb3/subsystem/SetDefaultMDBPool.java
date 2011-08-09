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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PoolConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueInjectionService;

import java.util.Locale;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

/**
 * User: jpai
 */
public class SetDefaultMDBPool implements OperationStepHandler, DescriptionProvider {

    public static final SetDefaultMDBPool INSTANCE = new SetDefaultMDBPool();

    @Override
    public ModelNode getModelDescription(Locale locale) {
        // TODO: Use Locale
        final ModelNode description = new ModelNode();
        description.get(DESCRIPTION).set("Sets the default MDB instance pool, " +
                "which will be applicable to all MDBs, unless overridden at the deployment or bean level");

        // setup the param descriptions
        description.get(REQUEST_PROPERTIES, NAME, DESCRIPTION).set("The pool name which refers to an already configured bean instance pool");
        description.get(REQUEST_PROPERTIES, NAME, TYPE).set(ModelType.STRING);
        description.get(REQUEST_PROPERTIES, NAME, REQUIRED).set(true);

        return description;
    }

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // get the pool name value from the operation's "name" param
        final String poolName = operation.require(EJB3SubsystemModel.NAME).asString();
        // update the model
        // first get the ModelNode for the address on which this operation was executed. i.e. /subsystem=ejb3
        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        model.get(EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL).set(poolName);

        // now create a runtime operation to update the default MDB pool config service
        context.addStep(new DefaultMDBPoolConfigServiceUpdateHandler(poolName), OperationContext.Stage.RUNTIME);
        // complete the step
        context.completeStep();
    }

    private class DefaultMDBPoolConfigServiceUpdateHandler implements OperationStepHandler {

        private final String poolName;

        DefaultMDBPoolConfigServiceUpdateHandler(final String poolName) {
            this.poolName = poolName;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
            ServiceController existingDefaultMDBPoolConfigService = serviceRegistry.getService(PoolConfigService.DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME);
            // if a default MDB pool is already installed, then remove it first
            if (existingDefaultMDBPoolConfigService != null) {
                existingDefaultMDBPoolConfigService.setMode(ServiceController.Mode.REMOVE);
            }
            // now install default MDB pool config service which points to an existing pool config service
            final ValueInjectionService<PoolConfig> newDefaultMDBPoolConfigService = new ValueInjectionService<PoolConfig>();
            context.getServiceTarget().addService(PoolConfigService.DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME, newDefaultMDBPoolConfigService)
                    .addDependency(PoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName), PoolConfig.class, newDefaultMDBPoolConfigService.getInjector())
                    .install();

            // complete the step
            context.completeStep();
        }
    }
}
