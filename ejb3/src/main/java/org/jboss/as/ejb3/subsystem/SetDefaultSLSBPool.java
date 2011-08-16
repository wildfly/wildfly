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
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PoolConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueInjectionService;

import java.util.Locale;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * User: jpai
 */
public class SetDefaultSLSBPool implements OperationStepHandler {

    public static final SetDefaultSLSBPool INSTANCE = new SetDefaultSLSBPool();

    @Override
    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        // get the pool name value from the operation's "value" param
        final String poolName = operation.require(ModelDescriptionConstants.VALUE).asString();
        // update the model
        // first get the ModelNode for the address on which this operation was executed. i.e. /subsystem=ejb3
        ModelNode model = context.readResourceForUpdate(PathAddress.EMPTY_ADDRESS).getModel();
        model.get(EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL).set(poolName);

        // now create a runtime operation to update the default slsb pool config service
        if (context.getType() == OperationContext.Type.SERVER) {
            context.addStep(new DefaultSLSBPoolConfigServiceUpdateHandler(poolName), OperationContext.Stage.RUNTIME);
        }

        // complete the step
        context.completeStep();
    }

    private class DefaultSLSBPoolConfigServiceUpdateHandler implements OperationStepHandler {

        private final String poolName;

        DefaultSLSBPoolConfigServiceUpdateHandler(final String poolName) {
            this.poolName = poolName;
        }

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
            ServiceController existingDefaultSLSBPoolConfigService = serviceRegistry.getService(PoolConfigService.DEFAULT_SLSB_POOL_CONFIG_SERVICE_NAME);
            // if a default SLSB pool is already installed, then remove it first
            if (existingDefaultSLSBPoolConfigService != null) {
                context.removeService(existingDefaultSLSBPoolConfigService);
            }
            // now install default slsb pool config service which points to an existing pool config service
            final ValueInjectionService<PoolConfig> newDefaultSLSBPoolConfigService = new ValueInjectionService<PoolConfig>();
            context.getServiceTarget().addService(PoolConfigService.DEFAULT_SLSB_POOL_CONFIG_SERVICE_NAME, newDefaultSLSBPoolConfigService)
                    .addDependency(PoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName), PoolConfig.class, newDefaultSLSBPoolConfigService.getInjector())
                    .install();

            // complete the step
            context.completeStep();
        }
    }
}
