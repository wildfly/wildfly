package org.jboss.as.domain.http.server;

import io.undertow.server.HttpHandler;

/**
 * @author Stuart Douglas
 */
public class ResourceHandlerDefinition {

    private final String context;
    private final String defaultPath;
    private final HttpHandler handler;

    public ResourceHandlerDefinition(final String context, final String defaultPath, final HttpHandler handler) {
        this.context = context;
        this.defaultPath = defaultPath;
        this.handler = handler;
    }

    public String getContext() {
        return context;
    }

    public String getDefaultPath() {
        return defaultPath;
    }

    public HttpHandler getHandler() {
        return handler;
    }
}
