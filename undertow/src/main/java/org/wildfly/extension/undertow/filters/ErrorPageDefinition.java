/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.error.FileErrorPageHandler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ErrorPageDefinition extends SimpleFilterDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.ERROR_PAGE);

    public static final AttributeDefinition CODE = new SimpleAttributeDefinitionBuilder("code", ModelType.INT)
            .setAllowExpression(true)
            .setRequired(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition PATH = new SimpleAttributeDefinitionBuilder("path", ModelType.STRING)
            .setAllowExpression(true)
            .setRequired(false)
            .setRestartAllServices()
            .build();
    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CODE, PATH);

    ErrorPageDefinition() {
        super(PATH_ELEMENT, ErrorPageDefinition::createHandlerWrapper);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException {
        int code = CODE.resolveModelAttribute(context, model).asInt();
        String path = PATH.resolveModelAttribute(context, model).asStringOrNull();
        return PredicateHandlerWrapper.filter(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler next) {
                return new FileErrorPageHandler(next, Paths.get(path), code);
            }
        });
    }
}
