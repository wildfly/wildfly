/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.messaging.activemq.jms;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.INCLUDE_DEFAULTS;
import static org.wildfly.extension.messaging.activemq.CommonAttributes.SERVER;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.jboss.as.controller.AbstractRuntimeOnlyHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.messaging.activemq._private.MessagingLogger;

/**
 * Base type for runtime operations on XML deployed message resources
 *
 * @author Stuart Douglas
 */
public abstract class AbstractJMSRuntimeHandler<T> extends AbstractRuntimeOnlyHandler {

    private final Map<ResourceConfig, T> resources = Collections.synchronizedMap(new HashMap<ResourceConfig, T>());

    @Override
    protected void executeRuntimeStep(OperationContext context, ModelNode operation) throws OperationFailedException {
        String opName = operation.require(ModelDescriptionConstants.OP).asString();
        PathAddress address = PathAddress.pathAddress(operation.require(ModelDescriptionConstants.OP_ADDR));
        final T dataSource = getResourceConfig(address);

        boolean includeDefault = operation.hasDefined(INCLUDE_DEFAULTS) ? operation.get(INCLUDE_DEFAULTS).asBoolean() : false;

        if (ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION.equals(opName)) {
            final String attributeName = operation.require(ModelDescriptionConstants.NAME).asString();
            executeReadAttribute(attributeName, context, dataSource, address, includeDefault);
        } else {
            throw unknownOperation(opName);
        }
    }

    public void registerResource(final String server, final String name, final T resource) {
        resources.put(new ResourceConfig(server, name), resource);
    }

    public void unregisterResource(final String server, final String name) {
        resources.remove(new ResourceConfig(server, name));
    }

    protected abstract void executeReadAttribute(final String attributeName, final OperationContext context, final T destination, final PathAddress address, final boolean includeDefault);

    private static IllegalStateException unknownOperation(String opName) {
        throw MessagingLogger.ROOT_LOGGER.operationNotValid(opName);
    }

    private T getResourceConfig(final PathAddress operationAddress) throws OperationFailedException {

        final String name = operationAddress.getLastElement().getValue();
        PathElement serverElt = operationAddress.getParent().getLastElement();
        final String server;
        if(serverElt != null && SERVER.equals(serverElt.getKey())) {
            server = serverElt.getValue();
        } else {
            server = "";
        }
        T config = resources.get(new ResourceConfig(server, name));

        if (config == null) {
            throw new OperationFailedException(MessagingLogger.ROOT_LOGGER.noDestinationRegisteredForAddress(operationAddress));
        }

        return config;
    }

    private static final class ResourceConfig {
        private final String server;
        private final String name;

        private ResourceConfig(final String server, final String name) {
            this.name = name;
            if (server == null) {
                this.server = "";
            } else {
                this.server = server;
            }
        }

        @Override
        public int hashCode() {
            int result = Objects.hashCode(this.server);
            result = 31 * result + Objects.hashCode(this.name);
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final ResourceConfig other = (ResourceConfig) obj;
            if (!Objects.equals(this.server, other.server)) {
                return false;
            }
            if (!Objects.equals(this.name, other.name)) {
                return false;
            }
            return true;
        }

    }

}
