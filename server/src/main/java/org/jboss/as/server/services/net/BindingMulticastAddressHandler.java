/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */
package org.jboss.as.server.services.net;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.InetAddressValidator;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;

/**
 * Handler for changing the interface on a socket binding.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class BindingMulticastAddressHandler extends AbstractBindingWriteHandler {

    public static final BindingMulticastAddressHandler INSTANCE = new BindingMulticastAddressHandler();

    private BindingMulticastAddressHandler() {
        super(new InetAddressValidator(true, true), new InetAddressValidator(true, false));
    }

    @Override
    protected boolean requiresRuntime(final OperationContext context) {
        return true;
    }

    @Override
    void handleRuntimeChange(ModelNode operation, String attributeName, ModelNode attributeValue, SocketBinding binding) throws OperationFailedException {
        final InetAddress address;
        if(attributeValue.isDefined()) {
            try {
                address = InetAddress.getByName(attributeValue.asString());
            } catch (UnknownHostException e) {
                throw new OperationFailedException(new ModelNode().set("failed to get multi-cast address for " + attributeValue));
            }
        } else {
            address = null;
        }
        binding.setMulticastAddress(address);
    }

    @Override
    void handleRuntimeRollback(ModelNode operation, String attributeName, ModelNode attributeValue, SocketBinding binding) {
        final InetAddress address;
        if(attributeValue.isDefined()) {
            try {
                address = InetAddress.getByName(attributeValue.asString());
            } catch (UnknownHostException e) {
                throw new RuntimeException("Failed to get multi-cast address for " + attributeValue.asString());
            }
        } else {
            address = null;
        }
        binding.setMulticastAddress(address);
    }
}
