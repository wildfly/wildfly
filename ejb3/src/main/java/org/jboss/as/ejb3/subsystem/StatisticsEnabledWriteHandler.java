/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
import org.jboss.as.ejb3.component.EJBUtilities;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.ejb3.subsystem.EJB3SubsystemRootResourceDefinition.STATISTICS_ENABLED;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
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
        utilities(context).setStatisticsEnabled(statisticsEnabled);
    }

    private static EJBUtilities utilities(final OperationContext context) {
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(true);
        return (EJBUtilities) serviceRegistry.getRequiredService(EJBUtilities.SERVICE_NAME).getValue();
    }
}
