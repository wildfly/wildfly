/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.List;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.handlers.RequestLimitingHandler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class RequestLimitHandlerDefinition extends SimpleFilterDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement("request-limit");

    public static final AttributeDefinition MAX_CONCURRENT_REQUESTS = new SimpleAttributeDefinitionBuilder("max-concurrent-requests", ModelType.INT)
            .setValidator(new IntRangeValidator(1, false, true))
            .setAllowExpression(true)
            .setRequired(true)
            .setRestartAllServices()
            .build();


    public static final AttributeDefinition QUEUE_SIZE = new SimpleAttributeDefinitionBuilder("queue-size", ModelType.INT)
            .setValidator(new IntRangeValidator(0, true, true))
            .setAllowExpression(true)
            .setRequired(false)
            .setDefaultValue(ModelNode.ZERO)
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(MAX_CONCURRENT_REQUESTS, QUEUE_SIZE);

    RequestLimitHandlerDefinition() {
        super(PATH_ELEMENT, RequestLimitHandlerDefinition::createHandlerWrapper);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static HandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException {
        int maxConcurrentRequests = MAX_CONCURRENT_REQUESTS.resolveModelAttribute(context, model).asInt();
        int queueSize = QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        return next -> new RequestLimitingHandler(maxConcurrentRequests, queueSize, next);
    }
}
