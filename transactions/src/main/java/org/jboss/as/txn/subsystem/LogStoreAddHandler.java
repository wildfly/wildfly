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
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="stefano.maestri@redhat.com">Stefano Maestri</a>
 */
class LogStoreAddHandler implements OperationStepHandler {
    private LogStoreResource resource = null;

    LogStoreAddHandler(LogStoreResource resource) {
        this.resource = resource;
    }

    @Override
    public void execute(final OperationContext context, final ModelNode operation) throws OperationFailedException {
        // Add the log store resource
        final ModelNode model = resource.getModel();

        LogStoreConstants.LOG_STORE_TYPE.validateAndSet(operation, model);

        context.addResource(PathAddress.EMPTY_ADDRESS, resource);
    }
}
