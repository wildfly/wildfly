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
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author Stuart Douglas
 */
public class EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler extends AbstractWriteAttributeHandler<Void> {


    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("ejb3", "entity-bean", "optimistic-locking");

    public static final EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler INSTANCE =
            new EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler();

    private EJB3SubsystemDefaultEntityBeanOptimisticLockingWriteHandler() {
        super(EJB3SubsystemRootResourceDefinition.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING);
    }

    @Override
    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                           ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateOptimisticLocking(context, model);

        return false;
    }

    @Override
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
                                         ModelNode valueToRestore, ModelNode valueToRevert, Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateOptimisticLocking(context, restored);
    }

    void updateOptimisticLocking(final OperationContext context, final ModelNode model) throws OperationFailedException {

        final ModelNode enabled = EJB3SubsystemRootResourceDefinition.DEFAULT_ENTITY_BEAN_OPTIMISTIC_LOCKING.resolveModelAttribute(context, model);

        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        ServiceController<?> existingService = serviceRegistry.getService(SERVICE_NAME);
        // if a default optimistic locking config is installed, remove it
        if (existingService != null) {
            context.removeService(existingService);
        }

        if (enabled.isDefined()) {
            final Service<Boolean> newDefaultPoolConfigService = new ValueService<Boolean>(new ImmediateValue<Boolean>(enabled.asBoolean()));
            ServiceController<?> newController =
                context.getServiceTarget().addService(SERVICE_NAME, newDefaultPoolConfigService)
                    .install();
        }

    }
}
