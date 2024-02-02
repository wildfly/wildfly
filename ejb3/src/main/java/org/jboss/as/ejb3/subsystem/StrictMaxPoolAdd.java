/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService.Derive;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfig;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.dmr.ModelNode;

/**
 * Adds a strict-max-pool to the EJB3 subsystem's bean-instance-pools. The {#performRuntime runtime action}
 * will create and install a {@link org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService}
 * <p/>
 * User: Jaikiran Pai
 */
public class StrictMaxPoolAdd extends AbstractAddStepHandler {

    static final String IO_MAX_THREADS_RUNTIME_CAPABILITY_NAME = "org.wildfly.io.max-threads";

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode strictMaxPoolModel) throws OperationFailedException {
        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.ADDRESS)).getLastElement().getValue();
        final int maxPoolSize = StrictMaxPoolResourceDefinition.MAX_POOL_SIZE.resolveModelAttribute(context, strictMaxPoolModel).asInt();
        final Derive derive = StrictMaxPoolResourceDefinition.parseDeriveSize(context, strictMaxPoolModel);
        final long timeout = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT.resolveModelAttribute(context, strictMaxPoolModel).asLong();
        final String unit = StrictMaxPoolResourceDefinition.INSTANCE_ACQUISITION_TIMEOUT_UNIT.resolveModelAttribute(context, strictMaxPoolModel).asString();

        // create and install the service
        CapabilityServiceTarget capabilityServiceTarget = context.getCapabilityServiceTarget();
        CapabilityServiceBuilder<?> sb = capabilityServiceTarget.addCapability(StrictMaxPoolResourceDefinition.STRICT_MAX_POOL_CONFIG_CAPABILITY);
        final Consumer<StrictMaxPoolConfig> configConsumer = sb.provides(StrictMaxPoolResourceDefinition.STRICT_MAX_POOL_CONFIG_CAPABILITY);
        Supplier<Integer> maxThreadsSupplier = null;
        if (context.hasOptionalCapability(IO_MAX_THREADS_RUNTIME_CAPABILITY_NAME, StrictMaxPoolResourceDefinition.STRICT_MAX_POOL_CONFIG_CAPABILITY.getDynamicName(context.getCurrentAddress()), null)) {
            maxThreadsSupplier = sb.requiresCapability(IO_MAX_THREADS_RUNTIME_CAPABILITY_NAME, Integer.class);
        }
        final StrictMaxPoolConfigService poolConfigService = new StrictMaxPoolConfigService(configConsumer, maxThreadsSupplier, poolName, maxPoolSize, derive, timeout, TimeUnit.valueOf(unit));
        sb.setInstance(poolConfigService);
        sb.install();
    }
}
