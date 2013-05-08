/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow.filters;

import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimitingHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
class ConnectionLimitHandler extends Filter {

    private static final AttributeDefinition MAX_CONCURRENT_REQUESTS = new SimpleAttributeDefinitionBuilder("max-concurrent-requests", ModelType.INT)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

    /*
    <connection-limit max-concurrent-requests="100" />
     */

    public ConnectionLimitHandler() {
        super("connection-limit");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(MAX_CONCURRENT_REQUESTS);
    }


    @Override
    public Class<? extends HttpHandler> getHandlerClass() {
        return RequestLimitingHandler.class;
    }
}
