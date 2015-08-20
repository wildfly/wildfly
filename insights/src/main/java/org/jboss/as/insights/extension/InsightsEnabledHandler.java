/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
package org.jboss.as.insights.extension;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:jkinlaw@redhat.com">Josh Kinlaw</a>
 */
public class InsightsEnabledHandler extends AbstractWriteAttributeHandler<Void> {

    public static final InsightsEnabledHandler INSTANCE = new InsightsEnabledHandler();

    private InsightsEnabledHandler() {
        super(InsightsSubsystemDefinition.ENABLED);
    }

    protected boolean applyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
            ModelNode resolvedValue, ModelNode currentValue, HandbackHolder<Void> handbackHolder) throws OperationFailedException {
        if (attributeName.equals(InsightsExtension.ENABLED)) {
            InsightsService service = (InsightsService) context.getServiceRegistry(true).getRequiredService(InsightsService.createServiceName()).getValue();
            service.setEnabled(resolvedValue.asBoolean());
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
        return false;
    }

    /**
     * Hook to allow subclasses to revert runtime changes made in
     * {@link #applyUpdateToRuntime(OperationContext, ModelNode, String, ModelNode, ModelNode, HandbackHolder)}.
     *
     * @param context        the context of the operation
     * @param operation      the operation
     * @param attributeName  the name of the attribute being modified
     * @param valueToRestore the previous value for the attribute, before this operation was executed
     * @param valueToRevert  the new value for the attribute that should be reverted
     * @param handback       an object, if any, passed in to the {@code handbackHolder} by the {@code applyUpdateToRuntime}
     *                       implementation
     */
    protected void revertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName,
          ModelNode valueToRestore, ModelNode valueToRevert, Void handback) {
        if (attributeName.equals(InsightsExtension.ENABLED)) {
            InsightsService service = (InsightsService) context.getServiceRegistry(true).getRequiredService(InsightsService.createServiceName()).getValue();
            service.setEnabled(valueToRestore.asBoolean());
            context.completeStep(OperationContext.ResultHandler.NOOP_RESULT_HANDLER);
        }
    }
}