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

package org.jboss.as.messaging.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;

import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.messaging.MessagingServices;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:andy.taylor@jboss.com">Andy Taylor</a>
 *         Date: 5/13/11
 *         Time: 3:30 PM
 */
public class PooledConnectionFactoryRemove extends AbstractRemoveStepHandler {

    public static final PooledConnectionFactoryRemove INSTANCE = new PooledConnectionFactoryRemove();



    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) {
        final ModelNode operationAddress = operation.require(OP_ADDR);
        final PathAddress address = PathAddress.pathAddress(operationAddress);
        final String name = address.getLastElement().getValue();

        context.removeService(MessagingServices.POOLED_CONNECTION_FACTORY_BASE.append(name));
    }

    protected void recoverServices(OperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
