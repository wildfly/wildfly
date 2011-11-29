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

package org.jboss.as.logging.handlers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FILTER;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Handler;

import org.jboss.as.controller.AbstractWriteAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.logging.util.LogServices;
import org.jboss.as.logging.util.ModelParser;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Date: 12.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public abstract class AbstractLogHandlerWriteAttributeHandler<T extends Handler> extends AbstractWriteAttributeHandler<T> {
    private final Map<String, AttributeDefinition> attributes;

    protected AbstractLogHandlerWriteAttributeHandler(final AttributeDefinition... attributes) {
        this(Arrays.asList(attributes));
    }

    protected AbstractLogHandlerWriteAttributeHandler(final Collection<AttributeDefinition> attributes) {
        this.attributes = new HashMap<String, AttributeDefinition>();
        this.attributes.put(LEVEL.getName(), LEVEL);
        this.attributes.put(FILTER.getName(), FILTER);
        this.attributes.put(FORMATTER.getName(), FORMATTER);
        this.attributes.put(ENCODING.getName(), ENCODING);
        this.attributes.put(FILTER.getName(), FILTER);
        for (AttributeDefinition attr : attributes) {
            this.attributes.put(attr.getName(), attr);
        }
    }

    @Override
    protected final boolean applyUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode resolvedValue, final ModelNode currentValue, final HandbackHolder<T> handbackHolder) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        @SuppressWarnings("unchecked")
        final ServiceController<T> controller = (ServiceController<T>) serviceRegistry.getService(LogServices.handlerName(name));
        if (controller == null) {
            return false;
        }
        // Attempt to cast handler
        @SuppressWarnings("unchecked")
        final T handler = controller.getValue();
        if (LEVEL.getName().equals(attributeName)) {
            handler.setLevel(ModelParser.parseLevel(resolvedValue));
        } else if (FILTER.getName().equals(attributeName)) {
            handler.setFilter(ModelParser.parseFilter(context, resolvedValue));
        } else if (FORMATTER.getName().equals(attributeName)) {
            FormatterSpec.fromModelNode(context, resolvedValue).apply(handler);
        } else if (ENCODING.getName().equals(attributeName)) {
            try {
                handler.setEncoding(resolvedValue.asString());
            } catch (UnsupportedEncodingException e) {
                throw new OperationFailedException(e, new ModelNode().set(MESSAGES.failedToSetHandlerEncoding()));
            }
        }
        return doApplyUpdateToRuntime(context, operation, attributeName, resolvedValue, currentValue, name, handler);
    }

    @Override
    protected final void revertUpdateToRuntime(final OperationContext context, final ModelNode operation, final String attributeName, final ModelNode valueToRestore, final ModelNode valueToRevert, final T handler) throws OperationFailedException {
        if (handler != null) {
            final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
            final String name = address.getLastElement().getValue();
            if (LEVEL.getName().equals(attributeName)) {
                handler.setLevel(ModelParser.parseLevel(valueToRestore));
            } else if (FILTER.getName().equals(attributeName)) {
                handler.setFilter(ModelParser.parseFilter(context, valueToRestore));
            } else if (FORMATTER.getName().equals(attributeName)) {
                FormatterSpec.fromModelNode(context, valueToRestore).apply(handler);
            } else if (ENCODING.getName().equals(attributeName)) {
                try {
                    handler.setEncoding(valueToRestore.asString());
                } catch (UnsupportedEncodingException e) {
                    throw new OperationFailedException(e, new ModelNode().set(MESSAGES.failedToSetHandlerEncoding()));
                }
            }
            doRevertUpdateToRuntime(context, operation, attributeName, valueToRestore, valueToRevert, name, handler);
        }
    }

    /**
     * Applies additional runtime attributes for the handler.
     *
     * @param context       the context for the operation.
     * @param operation     the operation
     * @param attributeName the name of the attribute being modified
     * @param resolvedValue the new value for the attribute, after {@link org.jboss.dmr.ModelNode#resolve()} has been called on it
     * @param currentValue  the existing value for the attribute
     * @param handlerName   the name of the handler.
     * @param handler       the {@link java.util.logging.Handler handler} to apply the changes to.
     *
     * @return {@code true} if the server requires restart to effect the attribute value change; {@code false} if not.
     *
     * @throws OperationFailedException if the operation fails.
     */
    protected abstract boolean doApplyUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode resolvedValue, ModelNode currentValue, String handlerName, T handler) throws OperationFailedException;

    /**
     * Reverts updates to the handler.
     *
     * @param context        the context for the operation.
     * @param operation      the operation
     * @param attributeName  the name of the attribute being modified
     * @param valueToRestore the previous value for the attribute, before this operation was executed
     * @param valueToRevert  the new value for the attribute that should be reverted
     * @param handlerName    the name of the handler.
     * @param handler        the handler to apply the changes to.
     *
     * @throws OperationFailedException if the operation fails.
     */
    protected abstract void doRevertUpdateToRuntime(OperationContext context, ModelNode operation, String attributeName, ModelNode valueToRestore, ModelNode valueToRevert, String handlerName, T handler) throws OperationFailedException;

    @Override
    protected final void validateResolvedValue(final String name, final ModelNode value) throws OperationFailedException {
        if (attributes.containsKey(name)) {
            attributes.get(name).getValidator().validateResolvedParameter(name, value);
        } else {
            super.validateResolvedValue(name, value);
        }
    }

    @Override
    protected final void validateUnresolvedValue(final String name, final ModelNode value) throws OperationFailedException {
        if (attributes.containsKey(name)) {
            attributes.get(name).getValidator().validateParameter(name, value);
        } else {
            super.validateUnresolvedValue(name, value);
        }
    }

    @Override
    protected AttributeDefinition getAttributeDefinition(final String attributeName) {
        return attributes == null ? null : attributes.get(attributeName);
    }

    /**
     * Returns a collection of attributes used for the write attribute.
     *
     * @return a collection of attributes.
     */
    public final Collection<AttributeDefinition> getAttributes() {
        return Collections.unmodifiableCollection(attributes.values());
    }
}
