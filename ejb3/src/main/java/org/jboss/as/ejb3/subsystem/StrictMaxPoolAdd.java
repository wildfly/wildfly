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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.PoolConfigService;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Adds a strict-max-pool to the EJB3 subsystem's bean-instance-pools. The {#performRuntime runtime action}
 * will create and install a {@link PoolConfigService}
 * <p/>
 * User: Jaikiran Pai
 */
public class StrictMaxPoolAdd extends AbstractAddStepHandler {

    public static final StrictMaxPoolAdd INSTANCE = new StrictMaxPoolAdd();

    /**
     * Populate the <code>strictMaxPoolModel</code> from the <code>operation</code>
     *
     * @param operation          the operation
     * @param strictMaxPoolModel strict-max-pool ModelNode
     * @throws OperationFailedException
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode strictMaxPoolModel) throws OperationFailedException {

        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        strictMaxPoolModel.get(EJB3SubsystemModel.NAME).set(poolName);

        for (AttributeDefinition attr : StrictMaxPoolResourceDefinition.ATTRIBUTES.values()) {
            attr.validateAndSet(operation, strictMaxPoolModel);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode strictMaxPoolModel,
                                  ServiceVerificationHandler verificationHandler,
                                  List<ServiceController<?>> serviceControllers) throws OperationFailedException {

        final ServiceController serviceController = installRuntimeService(context, strictMaxPoolModel, verificationHandler);
        // add this to the service controllers
        serviceControllers.add(serviceController);

    }

    ServiceController installRuntimeService(OperationContext context, ModelNode strictMaxPoolModel,
                                  ServiceVerificationHandler verificationHandler) throws OperationFailedException {

        final String poolName = strictMaxPoolModel.require(EJB3SubsystemModel.NAME).asString();
        final int maxPoolSize = StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.resolveModelAttribute(context, strictMaxPoolModel).asInt();
        final long timeout = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.resolveModelAttribute(context, strictMaxPoolModel).asLong();
        final String unit = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.resolveModelAttribute(context, strictMaxPoolModel).asString();
        // create the pool config
        final PoolConfig strictMaxPoolConfig = new StrictMaxPoolConfig(poolName, maxPoolSize, timeout, TimeUnit.valueOf(unit));
        // create and install the service
        final PoolConfigService poolConfigService = new PoolConfigService(strictMaxPoolConfig);
        final ServiceName serviceName = PoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName);
        ServiceBuilder<PoolConfig> svcBuilder = context.getServiceTarget().addService(serviceName, poolConfigService);
        if (verificationHandler != null) {
            svcBuilder.addListener(verificationHandler);
        }
        return svcBuilder.install();
    }

}
