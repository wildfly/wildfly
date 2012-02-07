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

package org.jboss.as.messaging.jms;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.messaging.MessagingMessages;
import org.jboss.dmr.ModelNode;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;

/**
 * Base type for runtime operations on XML deployed message destinations
 *
 * @author Stuart Douglas
 */
public abstract class AbstractJMSRuntimeHandler<T> extends AbstractRuntimeOnlyHandler {

    private final Map<DestinationConfig, T> destinations = Collections.synchronizedMap(new HashMap<DestinationConfig, T>());

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        String opName = operation.require(ModelDescriptionConstants.OP).asString();
        PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final T dataSource = getDestinationConfig(address);

        boolean includeDefault = operation.hasDefined(INCLUDE_DEFAULTS) ? operation.get(INCLUDE_DEFAULTS).asBoolean() : false;

        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeReadAttribute(attributeName, context, dataSource, address, includeDefault);
            context.completeStep();
        } else {
            throw unknownOperation(opName);
        }
    }

    public void registerDestination(final String server, final String name, final T destination) {
        destinations.put(new DestinationConfig(server, name), destination);
    }

    public void unregisterDestination(final String server, final String name) {
        destinations.remove(new DestinationConfig(server, name));
    }

    protected abstract void executeReadAttribute(final String attributeName, final OperationContext context, final T destination, final PathAddress address, final boolean includeDefault);

    private static IllegalStateException unknownOperation(String opName) {
        throw MessagingMessages.MESSAGES.operationNotValid(opName);
    }

    private T getDestinationConfig(final PathAddress operationAddress) throws OperationFailedException {

        final String name = operationAddress.getLastElement().getValue();
        final String server = operationAddress.getElement(operationAddress.size() - 2).getValue();

        T config = destinations.get(new DestinationConfig(server, name));

        if (config == null) {
            String exceptionMessage = MessagingMessages.MESSAGES.noDestinationRegisteredForAddress(operationAddress);
            throw new OperationFailedException(new ModelNode().set(exceptionMessage));
        }

        return config;
    }

    private static final class DestinationConfig {
        private final String server;
        private final String name;

        private DestinationConfig( final String server, final String name) {
            this.name = name;
            this.server = server;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final DestinationConfig that = (DestinationConfig) o;

            if (!name.equals(that.name)) return false;
            if (!server.equals(that.server)) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = server.hashCode();
            result = 31 * result + name.hashCode();
            return result;
        }
    }

}
