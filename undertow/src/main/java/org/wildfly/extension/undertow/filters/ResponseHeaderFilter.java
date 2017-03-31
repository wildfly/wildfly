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

import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.SetHeaderHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ResponseHeaderFilter extends Filter {

    public static final AttributeDefinition NAME = new SimpleAttributeDefinitionBuilder("header-name", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();
    public static final AttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder("header-value", ModelType.STRING)
            .setRequired(true)
            .setAllowExpression(true)
            .setRestartAllServices()
            .build();

    public static final ResponseHeaderFilter INSTANCE = new ResponseHeaderFilter();

    private ResponseHeaderFilter() {
        super("response-header");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(NAME, VALUE);
    }

    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return SetHeaderHandler.class;
    }


    @Override
    protected Class[] getConstructorSignature() {
        return new Class[] {HttpHandler.class, String.class, String.class};
    }
}
