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

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;

/**
 * Handler for changing the multicast-port on a socket binding.
 *
 * @author Emanuel Muckenhuber
 */
public class BindingMulticastPortHandler extends AbstractBindingWriteHandler {

    public static final BindingMulticastPortHandler INSTANCE = new BindingMulticastPortHandler();

    private BindingMulticastPortHandler() {
        super(new IntRangeValidator(1, 65535, true, true), new IntRangeValidator(1, 65535, true, false));
    }

    @Override
    protected boolean requiresRuntime(final OperationContext context) {
        return true;
    }

    @Override
    void handleRuntimeChange(ModelNode operation, String attributeName, ModelNode attributeValue, SocketBinding binding) throws OperationFailedException {
        binding.setMulticastPort(attributeValue.asInt());
    }

    @Override
    void handleRuntimeRollback(ModelNode operation, String attributeName, ModelNode attributeValue, SocketBinding binding) {
        binding.setMulticastPort(attributeValue.asInt());
    }
}
