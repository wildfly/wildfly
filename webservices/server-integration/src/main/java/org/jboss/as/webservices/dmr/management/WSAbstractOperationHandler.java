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

package org.jboss.as.webservices.dmr.management;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationHandler;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.controller.RuntimeTask;
import org.jboss.as.controller.RuntimeTaskContext;
import org.jboss.as.webservices.util.WSServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;

/**
 * Base class of all WS management DMR operation handlers.
 *
 * @author <a href="mailto:ema@redhat.com">Jim Ma</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class WSAbstractOperationHandler implements OperationHandler {

    private static final OperationResult RESULT = new BasicOperationResult();

    protected WSAbstractOperationHandler() {
        // intended for inheritance
    }

    /** {@inheritDoc} */
    @Override
    public final OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) throws OperationFailedException {
        if (context.getRuntimeContext() != null) {
            context.getRuntimeContext().setRuntimeTask(new RuntimeTask() {
                public void execute(RuntimeTaskContext context) throws OperationFailedException {
                    final ServiceController<?> controller = context.getServiceRegistry().getService(WSServices.REGISTRY_SERVICE);
                    if (controller != null) {
                        try {
                            final ModelNode result = getManagementOperationResultFragment(operation, controller);
                            resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, result);
                            resultHandler.handleResultComplete();
                        } catch (Exception e) {
                            throw new OperationFailedException(new ModelNode().set(getFallbackMessage() + ": " + e.getMessage()));
                        }
                    } else {
                        fallback(resultHandler, getFallbackMessage());
                    }
                }
            });
        } else {
            fallback(resultHandler, getFallbackMessage());
        }
        return RESULT;
    }

    protected abstract String getFallbackMessage();

    protected abstract ModelNode getManagementOperationResultFragment(final ModelNode operation, final ServiceController<?> controller) throws OperationFailedException;

    private static void fallback(final ResultHandler resultHandler, final String msg) {
        final ModelNode fallbackFragmenet = new ModelNode().set(msg);
        resultHandler.handleResultFragment(ResultHandler.EMPTY_LOCATION, fallbackFragmenet);
        resultHandler.handleResultComplete();
    }

}
