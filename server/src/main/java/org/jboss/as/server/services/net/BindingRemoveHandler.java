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
package org.jboss.as.server.services.net;

import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.PathAddress;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import org.jboss.as.controller.operations.common.SocketBindingRemoveHandler;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;

/**
 * Handler for the server socket-binding resource's remove operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BindingRemoveHandler extends SocketBindingRemoveHandler {

    public static final BindingRemoveHandler INSTANCE = new BindingRemoveHandler();

    private BindingRemoveHandler() {
    }

    protected void performRuntime(NewOperationContext context, ModelNode operation, ModelNode model) {
        PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        String name = address.getLastElement().getValue();
        context.removeService(SocketBinding.JBOSS_BINDING_NAME.append(name));
    }

    protected void recoverServices(NewOperationContext context, ModelNode operation, ModelNode model) {
        // TODO:  RE-ADD SERVICES
    }
}
