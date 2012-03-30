/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * Operation handler for removing transaction logs from the TM and from the subsystem model
 */
public class LogStoreTransactionDeleteHandler implements OperationStepHandler {
    // Generic failure reason if we cannot determine a more specific cause
    static final String LOG_DELETE_FAILURE_MESSAGE = "Unable to remove transaction log";

    private LogStoreResource logStoreResource = null;

    LogStoreTransactionDeleteHandler(LogStoreResource resource) {
        this.logStoreResource = resource;
    }

    public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
        MBeanServer mbs = TransactionExtension.getMBeanServer(context);
        final Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);

        try {
            final ObjectName on = LogStoreResource.getObjectName(resource);

            //  Invoke operation
            Object res = mbs.invoke(on, "remove", null, null);

            try {
                // validate that the MBean was removed:
                mbs.getObjectInstance(on);

                String reason = res != null ? res.toString() : LOG_DELETE_FAILURE_MESSAGE;

                throw new OperationFailedException(reason);
            } catch (InstanceNotFoundException e) {
                // success, the MBean was deleted
                final PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
                final PathElement element = address.getLastElement();

                context.acquireControllerLock();
                logStoreResource.removeChild(element);

                context.completeStep();
            }
        } catch (Exception e) {
            throw new OperationFailedException(e.getMessage());
        }

    }
}
