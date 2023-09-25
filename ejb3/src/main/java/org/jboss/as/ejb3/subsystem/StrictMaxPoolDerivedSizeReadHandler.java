/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class StrictMaxPoolDerivedSizeReadHandler extends AbstractRuntimeOnlyHandler{

    @Override
    protected void executeRuntimeStep(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        final String poolName = PathAddress.pathAddress(operation.get(ModelDescriptionConstants.OP_ADDR)).getLastElement().getValue();

        ServiceName serviceName = context.getCapabilityServiceName(StrictMaxPoolResourceDefinition.STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, poolName, StrictMaxPoolConfigService.class);
        final ServiceRegistry registry = context.getServiceRegistry(false);
        ServiceController<?> sc = registry.getService(serviceName);
        if (sc != null) {
            StrictMaxPoolConfigService smpc = (StrictMaxPoolConfigService) sc.getService();
            if (smpc != null) {
                context.getResult().set(smpc.getDerivedSize());
                return;
            }
        }
        throw EjbLogger.ROOT_LOGGER.cannotReadStrictMaxPoolDerivedSize(serviceName);
    }
}
