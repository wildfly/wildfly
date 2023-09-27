/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
