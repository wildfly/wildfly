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

package org.jboss.as.webservices.dmr;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.BasicOperationResult;
import org.jboss.as.controller.ModelRemoveOperationHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationResult;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ResultHandler;
import org.jboss.dmr.ModelNode;

/**
 * Removes WS endpoint from webservices subsystem model.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class WSEndpointRemove implements ModelRemoveOperationHandler {

    static final WSEndpointRemove INSTANCE = new WSEndpointRemove();

    private WSEndpointRemove() {
        // forbidden instantiation
    }

    @Override
    public OperationResult execute(final OperationContext context, final ModelNode operation, final ResultHandler resultHandler) {
        final ModelNode opAddr = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(opAddr);
        final String name = address.getLastElement().getValue();

        final ModelNode endpoint = context.getSubModel();
        endpoint.clear();

        /*
         * if (context.getRuntimeContext() != null) {
         * context.getRuntimeContext().setRuntimeTask(new RuntimeTask() { public
         * void execute(RuntimeTaskContext context) throws
         * OperationFailedException { final ServiceController<?> controller =
         * context.getServiceRegistry()
         * .getService(ThreadsServices.threadFactoryName(name)); if (controller
         * != null) { controller.setMode(ServiceController.Mode.REMOVE); }
         * resultHandler.handleResultComplete(); } }); } else {
         * resultHandler.handleResultComplete(); }
         */
        return new BasicOperationResult();
    }

}
