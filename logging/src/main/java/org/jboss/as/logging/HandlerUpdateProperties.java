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

package org.jboss.as.logging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.logging.CommonAttributes.ENCODING;
import static org.jboss.as.logging.CommonAttributes.FORMATTER;
import static org.jboss.as.logging.CommonAttributes.LEVEL;
import static org.jboss.as.logging.LoggingMessages.MESSAGES;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Handler;

import org.jboss.as.controller.AbstractModelUpdateHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.ServiceVerificationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceRegistry;

/**
 * Parent operation responsible for updating the common attributes of logging handlers.
 *
 * @author John Bailey
 */
public abstract class HandlerUpdateProperties<T extends Handler> extends AbstractModelUpdateHandler {
    static final String OPERATION_NAME = "update-properties";

    private final Set<String> attributes;
    private final List<AttributeDefinition> attributeDefinitions;

    protected HandlerUpdateProperties(final List<String> attributes, final List<? extends AttributeDefinition> attributeDefinitions) {
        this.attributes = new HashSet<String>(attributes);
        this.attributes.addAll(attributes);
        this.attributeDefinitions = new ArrayList<AttributeDefinition>();
        this.attributeDefinitions.add(ENCODING);
        this.attributeDefinitions.add(FORMATTER);
        this.attributeDefinitions.add(LEVEL);
        // TODO - support filter
        this.attributeDefinitions.addAll(attributeDefinitions);
    }

    protected HandlerUpdateProperties(final String... attributes) {
        this(Arrays.asList(attributes), Collections.<AttributeDefinition>emptyList());
    }

    protected HandlerUpdateProperties(final List<? extends AttributeDefinition> attributeDefinitions) {
        this(Collections.<String>emptyList(), attributeDefinitions);
    }

    @Override
    protected final void updateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        for (AttributeDefinition attr : attributeDefinitions) {
            attr.validateAndSet(operation, model);
        }
        for (String attr : attributes) {
            copy(attr, operation, model);
        }
    }

    @Override
    protected final void performRuntime(final OperationContext context, final ModelNode operation, final ModelNode model,
                                        final ServiceVerificationHandler verificationHandler, final List<ServiceController<?>> newControllers) throws OperationFailedException {
        final PathAddress address = PathAddress.pathAddress(operation.require(OP_ADDR));
        final String name = address.getLastElement().getValue();
        final ServiceRegistry serviceRegistry = context.getServiceRegistry(false);
        final ServiceController<Handler> controller = (ServiceController<Handler>) serviceRegistry.getService(LogServices.handlerName(name));
        if (controller != null) {
            final T handler = (T) controller.getValue();
            final ModelNode level = LEVEL.resolveModelAttribute(context, model);
            final ModelNode formatter = FORMATTER.resolveModelAttribute(context, model);
            final ModelNode encoding = ENCODING.resolveModelAttribute(context, model);
            // TODO (jrp) implement filter

            if (level.isDefined()) {
                handler.setLevel(java.util.logging.Level.parse(level.asString()));
            }

            if (formatter.isDefined()) {
                AbstractFormatterSpec.fromModelNode(context, model).apply(handler);
            }

            if (encoding.isDefined()) {
                try {
                    handler.setEncoding(encoding.asString());
                } catch (UnsupportedEncodingException e) {
                    throw new OperationFailedException(e, new ModelNode().set(MESSAGES.failedToSetHandlerEncoding()));
                }
            }
            updateRuntime(context, model, handler);
        }
    }

    protected abstract void updateRuntime(OperationContext context, final ModelNode operation, final T handler) throws OperationFailedException;

    /**
     * Copies the attribute, represented by the {@code name} parameter, from one {@link ModelNode} to another if the
     * {@link ModelNode from} parameter has the attributed defined. If the attribute was not defined, nothing happens.
     *
     * @param name the name of the attribute to copy.
     * @param from the model node to copy the value from.
     * @param to   the model node to copy the value to.
     */
    protected void copy(final String name, final ModelNode from, final ModelNode to) {
        if (from.hasDefined(name)) {
            to.get(name).set(from.get(name));
        }
    }

    /**
     * Returns a collection of attributes used for the write attribute.
     *
     * @return a collection of attributes.
     */
    public final Collection<AttributeDefinition> getAttributes() {
        return Collections.unmodifiableCollection(attributeDefinitions);
    }
}
