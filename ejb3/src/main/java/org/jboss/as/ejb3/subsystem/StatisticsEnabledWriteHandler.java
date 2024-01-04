/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.STATISTICS_ENABLED;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class StatisticsEnabledWriteHandler extends AbstractWriteAttributeHandler<Void> {
    static StatisticsEnabledWriteHandler INSTANCE = new StatisticsEnabledWriteHandler();

    StatisticsEnabledWriteHandler(){
        super(STATISTICS_ENABLED);
    }

    @Override
    protected boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<Void> voidHandbackHolder) throws OperationFailedException {
        final ModelNode model = context.readResource(PathAddress.EMPTY_ADDRESS).getModel();
        updateToRuntime(context, model);
        return false;
    }

    @Override
    protected void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final Void handback) throws OperationFailedException {
        final ModelNode restored = context.readResource(PathAddress.EMPTY_ADDRESS).getModel().clone();
        restored.get(attributeName).set(valueToRestore);
        updateToRuntime(context, restored);
    }

    void updateToRuntime(final OperationContext context, final ModelNode model) throws OperationFailedException {
        final boolean statisticsEnabled = STATISTICS_ENABLED.resolveModelAttribute(context, model).asBoolean();
        EJBStatistics.getInstance().setEnabled(statisticsEnabled);
    }
}
