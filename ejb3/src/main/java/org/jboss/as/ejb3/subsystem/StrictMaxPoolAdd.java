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

import static org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService.Derive;

import java.util.concurrent.TimeUnit;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;

/**
 * Adds a strict-max-pool to the EJB3 subsystem's bean-instance-pools. The {#performRuntime runtime action}
 * will create and install a {@link org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService}
 * <p/>
 * User: Jaikiran Pai
 */
public class StrictMaxPoolAdd extends AbstractAddStepHandler {

    public static final StrictMaxPoolAdd INSTANCE = new StrictMaxPoolAdd();

    static final String IO_MAX_THREADS_RUNTIME_CAPABILITY_NAME = "org.wildfly.io.max-threads";


    /**
     * Populate the <code>strictMaxPoolModel</code> from the <code>operation</code>
     *
     * @param operation          the operation
     * @param strictMaxPoolModel strict-max-pool ModelNode
     * @throws OperationFailedException
     */
    @Override
    protected void populateModel(ModelNode operation, ModelNode strictMaxPoolModel) throws OperationFailedException {
        for (AttributeDefinition attr : StrictMaxPoolResourceDefinition.ATTRIBUTES.values()) {
            attr.validateAndSet(operation, strictMaxPoolModel);
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode strictMaxPoolModel) throws OperationFailedException {

        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        final int maxPoolSize = StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.resolveModelAttribute(context, strictMaxPoolModel).asInt();
        final Derive derive = StrictMaxPoolResourceDefinition.parseDeriveSize(context, strictMaxPoolModel);
        final long timeout = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.resolveModelAttribute(context, strictMaxPoolModel).asLong();
        final String unit = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.resolveModelAttribute(context, strictMaxPoolModel).asString();
        // create and install the service
        final StrictMaxPoolConfigService poolConfigService = new StrictMaxPoolConfigService(poolName, maxPoolSize, derive, timeout, TimeUnit.valueOf(unit));


        final ServiceName serviceName = StrictMaxPoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName);
        ServiceBuilder<StrictMaxPoolConfig> svcBuilder = context.getServiceTarget().addService(serviceName, poolConfigService);

        if (context.hasOptionalCapability(IO_MAX_THREADS_RUNTIME_CAPABILITY_NAME, null, null)) {
            ServiceName name = context.getCapabilityServiceName(IO_MAX_THREADS_RUNTIME_CAPABILITY_NAME, Integer.class);
            svcBuilder.addDependency(name, Integer.class, poolConfigService.getMaxThreadsInjector());
        }

        svcBuilder.install();
    }

}
