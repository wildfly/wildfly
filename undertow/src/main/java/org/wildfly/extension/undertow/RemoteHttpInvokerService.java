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

package org.wildfly.extension.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.CookieImpl;
import io.undertow.server.session.SecureRandomSessionIdGenerator;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import io.undertow.server.handlers.PathHandler;
import org.wildfly.httpclient.common.HttpServiceConfig;

/**
 * Service that exposes the Wildfly Remote HTTP invoker base PathHandler.
 *
 * @author Stuart Douglas
 * @author Richard Achmatowicz
 */
public class RemoteHttpInvokerService implements Service<PathHandler> {

    private static final String COMMON_PATH = "/common";
    private static final String AFFINITY_PATH = "/affinity";

    private final PathHandler pathHandler = new PathHandler();

    @Override
    public void start(StartContext context) throws StartException {
        pathHandler.clearPaths();

        // add in a handler for servicing affinity requests from Wildfly HTTP client
        pathHandler.addPrefixPath(COMMON_PATH, getAffinityServiceHandler());
    }

    /**
     * An HttpHandler for handling affinity requests from a Wildfly HTTP client application.
     *
     * This handler is wrapped by HttpServiceConfig.wrap() to allow it to participate in the ee interoperability
     * protocol, used by HTTP client applications to permit interoperability between jakarta and javax clients
     * and servers.
     *
     * @return the HttpHandler
     */
    private HttpHandler getAffinityServiceHandler() {
        PathHandler wrappedHandler = new PathHandler();
        SecureRandomSessionIdGenerator generator = new SecureRandomSessionIdGenerator();
        wrappedHandler.addPrefixPath(AFFINITY_PATH, exchange -> {
            String resolved = exchange.getResolvedPath();
            int index = resolved.lastIndexOf(COMMON_PATH);
            if(index > 0) {
                resolved = resolved.substring(0, index);
            }
            exchange.getResponseCookies().put("JSESSIONID", new CookieImpl("JSESSIONID", generator.createSessionId()).setPath(resolved));
        });
        return HttpServiceConfig.getInstance().wrap(wrappedHandler);
    }

    @Override
    public void stop(StopContext context) {
        pathHandler.clearPaths();
    }

    @Override
    public PathHandler getValue() throws IllegalStateException, IllegalArgumentException {
        return pathHandler;
    }
}
