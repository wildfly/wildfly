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

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.ejb3.component.pool.PoolConfig;
import org.jboss.as.ejb3.component.pool.StrictMaxPoolConfigService;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueInjectionService;

/**
 * User: jpai
 */
public class EJB3SubsystemDefaultPoolWriteHandler extends AbstractWriteAttributeHandler<Void> {

    public static final EJB3SubsystemDefaultPoolWriteHandler MDB_POOL =
            new EJB3SubsystemDefaultPoolWriteHandler(StrictMaxPoolConfigService.DEFAULT_MDB_POOL_CONFIG_SERVICE_NAME,
                    EJB3SubsystemRootResourceDefinition.DEFAULT_MDB_INSTANCE_POOL);

    public static final EJB3SubsystemDefaultPoolWriteHandler SLSB_POOL =
            new EJB3SubsystemDefaultPoolWriteHandler(StrictMaxPoolConfigService.DEFAULT_SLSB_POOL_CONFIG_SERVICE_NAME,
                    EJB3SubsystemRootResourceDefinition.DEFAULT_SLSB_INSTANCE_POOL);

    public static final EJB3SubsystemDefaultPoolWriteHandler ENTITY_BEAN_POOL =
            new EJB3SubsystemDefaultPoolWriteHandler(StrictMaxPoolConfigService.DEFAULT_ENTITY_POOL_CONFIG_SERVICE_NAME,
                    EJB3SubsystemRootResourceDefinition.DEFAULT_ENTITY_BEAN_INSTANCE_POOL);

    private final ServiceName poolConfigServiceName;
    private final AttributeDefinition poolAttribute;

    public EJB3SubsystemDefaultPoolWriteHandler(ServiceName poolConfigServiceName, AttributeDefinition poolAttribute) {
        super(poolAttribute);
        this.poolConfigServiceName = poolConfigServiceName;
        this.poolAttribute = poolAttribute;
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

        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        ServiceController<?> existingDefaultPoolConfigService = serviceRegistry.getService(poolConfigServiceName);
        // if a default MDB pool is already installed, then remove it first
        if (existingDefaultPoolConfigService != null) {
            context.removeService(existingDefaultPoolConfigService);
        }

        if (poolName.isDefined()) {
            // now install default pool config service which points to an existing pool config service
            final ValueInjectionService<PoolConfig> newDefaultPoolConfigService = new ValueInjectionService<PoolConfig>();
            ServiceController<?> newController =
                context.getServiceTarget().addService(poolConfigServiceName, newDefaultPoolConfigService)
                    .addDependency(StrictMaxPoolConfigService.EJB_POOL_CONFIG_BASE_SERVICE_NAME.append(poolName.asString()),
                            PoolConfig.class, newDefaultPoolConfigService.getInjector())
                    .install();
        }

    }
}
