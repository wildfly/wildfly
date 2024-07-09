/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.List;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
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

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException {
        int maxConcurrentRequests = MAX_CONCURRENT_REQUESTS.resolveModelAttribute(context, model).asInt();
        int queueSize = QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        return PredicateHandlerWrapper.filter(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler next) {
                return new RequestLimitingHandler(maxConcurrentRequests, queueSize, next);
            }
        });
    }
}
