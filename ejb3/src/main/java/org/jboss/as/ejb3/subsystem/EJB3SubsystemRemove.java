/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME;
import static org.jboss.as.ejb3.subsystem.StrictMaxPoolResourceDefinition.STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME;

/**
 * Handler for removing the EJB3 subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class EJB3SubsystemRemove extends AbstractRemoveStepHandler {

    public static final EJB3SubsystemRemove INSTANCE = new EJB3SubsystemRemove();

    private EJB3SubsystemRemove() {
    }

    /*
     * handle conditionally-defined capabilities for default bean instance pools
     */
    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, ModelNode operation, Resource resource) throws OperationFailedException {
        ModelNode model = resource.getModel();

        // de-register the capability we are exporting as well as its capability requirement on the strict-max-pool that supports it
        if (model.hasDefined(DEFAULT_SLSB_INSTANCE_POOL)) {
            context.deregisterCapability(DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME);

            try {
                // need to resolve the attribute value before using it
                String resolvedDefaultSLSBPoolName = context.resolveExpressions(model.get(DEFAULT_SLSB_INSTANCE_POOL)).asString();
                String defaultSLSBPoolRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, resolvedDefaultSLSBPoolName);

                context.deregisterCapabilityRequirement(defaultSLSBPoolRequirementName, DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_SLSB_INSTANCE_POOL);
            } catch(OperationFailedException ofe) {
                EjbLogger.ROOT_LOGGER.defaultPoolExpressionCouldNotBeResolved(DEFAULT_SLSB_INSTANCE_POOL, model.get(DEFAULT_SLSB_INSTANCE_POOL).asString());
            }
        }

        if (model.hasDefined(DEFAULT_MDB_INSTANCE_POOL)) {
            context.deregisterCapability(DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME);

            try {
                // need to resolve the attribute value before using it
                String resolvedDefaultMDBPoolName = context.resolveExpressions(model.get(DEFAULT_MDB_INSTANCE_POOL)).asString();
                String defaultMDBPoolRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, resolvedDefaultMDBPoolName);

                context.deregisterCapabilityRequirement(defaultMDBPoolRequirementName, DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_MDB_INSTANCE_POOL);
            } catch(OperationFailedException ofe) {
                EjbLogger.ROOT_LOGGER.defaultPoolExpressionCouldNotBeResolved(DEFAULT_MDB_INSTANCE_POOL, model.get(DEFAULT_MDB_INSTANCE_POOL).asString());
            }
        }

        super.recordCapabilitiesAndRequirements(context, operation, resource);
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        // This subsystem registers DUPs, so we can't remove it from the runtime without a reload
        context.reloadRequired();
    }

    @Override
    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        context.revertReloadRequired();
    }
}
