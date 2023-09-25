/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_MDB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemModel.DEFAULT_SLSB_INSTANCE_POOL;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_POOL_CONFIG_CAPABILITY;
import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_POOL_CONFIG_CAPABILITY;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller._private.OperationFailedRuntimeException;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.logging.EjbLogger;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * User: jpai
 */
public class EJB3SubsystemDefaultPoolWriteHandler extends AbstractWriteAttributeHandler<Void> {

    private static final String STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config";
    private static final String DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config.slsb-default";
    private static final String DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config.mdb-default";
    private static final String DEFAULT_ENTITY_POOL_CONFIG_CAPABILITY_NAME = "org.wildfly.ejb3.pool-config.entity-default";

    public static final EJB3SubsystemDefaultPoolWriteHandler SLSB_POOL =
            new EJB3SubsystemDefaultPoolWriteHandler(DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME, EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_INSTANCE_POOL);

    public static final EJB3SubsystemDefaultPoolWriteHandler MDB_POOL =
            new EJB3SubsystemDefaultPoolWriteHandler(DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME, EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_INSTANCE_POOL);

    public static final EJB3SubsystemDefaultPoolWriteHandler ENTITY_BEAN_POOL =
            new EJB3SubsystemDefaultPoolWriteHandler(DEFAULT_ENTITY_POOL_CONFIG_CAPABILITY_NAME, EJB3SubsystemRootResourceDefinition.DEFAULT_ENTITY_BEAN_INSTANCE_POOL);

    private final String poolConfigCapabilityName;
    private final AttributeDefinition poolAttribute;

    public EJB3SubsystemDefaultPoolWriteHandler(String defaultPoolConfigCapabilityName, AttributeDefinition poolAttribute) {
        super(poolAttribute);
        this.poolConfigCapabilityName = defaultPoolConfigCapabilityName;
        this.poolAttribute = poolAttribute;
    }

    /*
     * Update the conditional capabilities for the default bean instance pools if the attribute values have changed
     * This write handler is registered with the EJB3SubsystemRootResource
     */
    @Override
    protected void recordCapabilitiesAndRequirements(OperationContext context, AttributeDefinition attributeDefinition, ModelNode newValue, ModelNode oldValue) {
        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        ModelNode model = resource.getModel();

        // de-register the default bean instance pool capability requirement on the default bean instance pool that supports it
        if (poolAttribute.getName().equals(DEFAULT_SLSB_INSTANCE_POOL)) {
            if (oldValue.isDefined()) {

                // NOTE: the new value may contain an expression, so we need to resolve it first
                String newSLSBRequirementName = null;
                try {
                    if (newValue.isDefined()) {
                        ModelNode resolvedNewValue = context.resolveExpressions(newValue);

                        // register the new requirement
                        newSLSBRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, resolvedNewValue.asString());
                        context.registerAdditionalCapabilityRequirement(newSLSBRequirementName, DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_SLSB_INSTANCE_POOL);
                    }
                } catch (OperationFailedException ofe) {
                    // if the new value cannot be resolved, deregister the old value only (in the finally clause)
                    EjbLogger.ROOT_LOGGER.defaultPoolExpressionCouldNotBeResolved(DEFAULT_SLSB_INSTANCE_POOL, model.get(DEFAULT_SLSB_INSTANCE_POOL).asString());
                } finally {
                    // de-register the old requirement
                    // if running write-attribute with the same value as the existing one, de-registering the old one
                    // would end up de-registering the new one. So de-register only if they are different.
                    String oldSLSBRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, oldValue.asString());
                    if (!oldSLSBRequirementName.equals(newSLSBRequirementName)) {
                        context.deregisterCapabilityRequirement(oldSLSBRequirementName, DEFAULT_SLSB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_SLSB_INSTANCE_POOL);
                    }
                }
            } else {
                if (newValue.isDefined()) {
                    try {
                        context.registerCapability(DEFAULT_SLSB_POOL_CONFIG_CAPABILITY);
                    } catch (OperationFailedRuntimeException e) {
                        //ignore, the capability already registered
                    }
                }
            }
        } else if (poolAttribute.getName().equals(DEFAULT_MDB_INSTANCE_POOL)) {
            if (oldValue.isDefined()) {

                // NOTE: the new value may contain an expression, so we need to resolve it first
                String newMDBRequirementName = null;
                try {
                    if (newValue.isDefined()) {
                        ModelNode resolvedNewValue = context.resolveExpressions(newValue);

                        // register the new requirement
                        newMDBRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, resolvedNewValue.asString());
                        context.registerAdditionalCapabilityRequirement(newMDBRequirementName, DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_MDB_INSTANCE_POOL);
                    }
                } catch (OperationFailedException ofe) {
                    // if the new value cannot be resolved, deregister the old value only (in the finally clause)
                    EjbLogger.ROOT_LOGGER.defaultPoolExpressionCouldNotBeResolved(DEFAULT_MDB_INSTANCE_POOL, model.get(DEFAULT_MDB_INSTANCE_POOL).asString());
                } finally {
                    // de-register the old requirement
                    // if running write-attribute with the same value as the existing one, de-registering the old one
                    // would end up de-registering the new one. So de-register only if they are different.
                    String oldMDBRequirementName = RuntimeCapability.buildDynamicCapabilityName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, oldValue.asString());
                    if (!oldMDBRequirementName.equals(newMDBRequirementName)) {
                        context.deregisterCapabilityRequirement(oldMDBRequirementName, DEFAULT_MDB_POOL_CONFIG_CAPABILITY_NAME, DEFAULT_MDB_INSTANCE_POOL);
                    }
                }
            } else {
                if (newValue.isDefined()) {
                    try {
                        context.registerCapability(DEFAULT_MDB_POOL_CONFIG_CAPABILITY);
                    } catch (OperationFailedRuntimeException e) {
                        //ignore, the capability already registered
                    }
                }
            }
        }
        super.recordCapabilitiesAndRequirements(context, attributeDefinition, newValue, oldValue);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updatePoolService(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updatePoolService(context, restored);
    }

    void updatePoolService(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode poolName = poolAttribute.resolveModelAttribute(context, model);

        ServiceName poolConfigServiceName = context.getCapabilityServiceName(this.poolConfigCapabilityName, PoolConfig.class);

        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        ServiceController<?> existingDefaultPoolConfigService = serviceRegistry.getService(poolConfigServiceName);
        // if a default MDB pool is already installed, then remove it first
        if (existingDefaultPoolConfigService != null) {
            context.removeService(existingDefaultPoolConfigService);
        }

        if (poolName.isDefined()) {
            // now install default pool config service which points to an existing pool config service
            final ServiceName poolConfigDependencyServiceName = context.getCapabilityServiceName(STRICT_MAX_POOL_CONFIG_CAPABILITY_NAME, PoolConfig.class, poolName.asString());
            final ServiceBuilder<?> sb = context.getServiceTarget().addService(poolConfigServiceName);
            final Consumer<PoolConfig> poolConfigConsumer = sb.provides(poolConfigServiceName);
            final Supplier<PoolConfig> poolConfigSupplier = sb.requires(poolConfigDependencyServiceName);
            sb.setInstance(new DefaultPoolConfigService(poolConfigConsumer, poolConfigSupplier)).install();
        }
    }
    private static final class DefaultPoolConfigService implements Service {
        private final Consumer<PoolConfig> poolConfigConsumer;
        private final Supplier<PoolConfig> poolConfigSupplier;
        private DefaultPoolConfigService(final Consumer<PoolConfig> poolConfigConsumer, final Supplier<PoolConfig> poolConfigSupplier) {
            this.poolConfigConsumer = poolConfigConsumer;
            this.poolConfigSupplier = poolConfigSupplier;
        }

        @Override
        public void start(final StartContext startContext) {
            poolConfigConsumer.accept(poolConfigSupplier.get());
        }

        @Override
        public void stop(final StopContext stopContext) {
            poolConfigConsumer.accept(null);
        }
    }
}
