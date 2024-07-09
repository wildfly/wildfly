/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.Collection;
import java.util.List;

import io.undertow.attribute.ExchangeAttributes;
import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.SetAttributeHandler;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

/**
 * @author Stuart Douglas
 */
public class RewriteFilterDefinition extends SimpleFilterDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement(Constants.REWRITE);

    public static final AttributeDefinition TARGET = new SimpleAttributeDefinitionBuilder("target", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();


    public static final AttributeDefinition REDIRECT = new SimpleAttributeDefinitionBuilder("redirect", ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(ModelNode.FALSE)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(TARGET, REDIRECT);

    RewriteFilterDefinition() {
        super(PATH_ELEMENT, RewriteFilterDefinition::createHandlerWrapper);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException {
        String target = TARGET.resolveModelAttribute(context, model).asString();
        boolean redirect = REDIRECT.resolveModelAttribute(context, model).asBoolean();
        return PredicateHandlerWrapper.filter(new HandlerWrapper() {
            @Override
            public HttpHandler wrap(HttpHandler next) {
                return redirect ? new RedirectHandler(target) : new SetAttributeHandler(next, ExchangeAttributes.relativePath(), ExchangeAttributes.parser(RewriteFilterDefinition.class.getClassLoader()).parse(target));
            }
        });
    }
}
