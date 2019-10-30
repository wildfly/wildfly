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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.error.FileErrorPageHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ErrorPageDefinition extends Filter{

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
    public static final Collection<AttributeDefinition> ATTRIBUTES = Collections.unmodifiableCollection(Arrays.asList(CODE, PATH));
    public static final ErrorPageDefinition INSTANCE = new ErrorPageDefinition();

    private ErrorPageDefinition() {
        super(Constants.ERROR_PAGE);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return FileErrorPageHandler.class;
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, ModelNode model, HttpHandler next) {
        int code = model.get(CODE.getName()).asInt();
        String path = model.get(PATH.getName()).asString();
        FileErrorPageHandler handler = new FileErrorPageHandler(Paths.get(path), code);
        handler.setNext(next);
        if(predicate == null) {
            return handler;
        } else {
            return Handlers.predicate(predicate, handler, next);
        }

    }

    @Override
    protected Class[] getConstructorSignature() {
        throw new IllegalStateException(); //should not be used, as the handler is constructed above
    }
}
