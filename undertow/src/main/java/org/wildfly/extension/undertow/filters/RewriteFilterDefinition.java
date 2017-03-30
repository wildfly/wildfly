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

import io.undertow.Handlers;
import io.undertow.attribute.ExchangeAttributes;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RedirectHandler;
import io.undertow.server.handlers.SetAttributeHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Stuart Douglas
 */
public class RewriteFilterDefinition extends Filter {

    public static final AttributeDefinition TARGET = new SimpleAttributeDefinitionBuilder("target", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();


    public static final AttributeDefinition REDIRECT = new SimpleAttributeDefinitionBuilder("redirect", ModelType.BOOLEAN)
            .setRequired(false)
            .setDefaultValue(new ModelNode(false))
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final RewriteFilterDefinition INSTANCE = new RewriteFilterDefinition();

    private RewriteFilterDefinition() {
        super(Constants.REWRITE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(TARGET, REDIRECT);
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, ModelNode model, HttpHandler next) {
        String expression = model.get(TARGET.getName()).asString();
        boolean redirect = model.get(REDIRECT.getName()).asBoolean();
        if(predicate == null) {
            return create(next, expression, redirect);
        } else {
            return Handlers.predicate(predicate, create(next, expression, redirect), next);
        }
    }

    public HttpHandler create(HttpHandler next, String expression, boolean redirect) {
        if(redirect) {
            return new RedirectHandler(expression);
        } else {
            return new SetAttributeHandler(next, ExchangeAttributes.relativePath(), ExchangeAttributes.parser(getClass().getClassLoader()).parse(expression));
        }
    }

    @Override
    protected Class[] getConstructorSignature() {
        throw new IllegalStateException(); //should not be used, as the handler is constructed above
    }
}
