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
