/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.osgi.parser;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;

/**
 * @author David Bosschaert
 * @author Thomas.Diesler@jboss.com
 */
public class OSGiFrameworkPropertyRemove extends AbstractRemoveStepHandler {
    static final OSGiFrameworkPropertyRemove INSTANCE = new OSGiFrameworkPropertyRemove();

    private OSGiFrameworkPropertyRemove() {
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        final String propName = operation.get(ModelDescriptionConstants.OP_ADDR).asObject().get(ModelConstants.PROPERTY).asString();
        final SubsystemState subsystemState = SubsystemState.getSubsystemState(context);
        if (subsystemState == null) {
            // cannot complete
            context.setRollbackOnly();
            context.completeStep(OperationContext.RollbackHandler.NOOP_ROLLBACK_HANDLER);
            return;
        }

        final Object oldVal = subsystemState.setProperty(propName, null);
        context.completeStep(new OperationContext.RollbackHandler() {
            @Override
            public void handleRollback(OperationContext context, ModelNode operation) {
                subsystemState.setProperty(propName, oldVal);
            }
        });
    }
}
