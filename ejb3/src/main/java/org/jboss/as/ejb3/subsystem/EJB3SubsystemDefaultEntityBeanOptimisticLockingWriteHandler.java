/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

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
            final ServiceBuilder<?> sb = context.getServiceTarget().addService(SERVICE_NAME);
            final Consumer<Boolean> c = sb.provides(SERVICE_NAME);
            sb.setInstance(Service.newInstance(c, enabled.asBoolean())).install();
        }
    }
}
