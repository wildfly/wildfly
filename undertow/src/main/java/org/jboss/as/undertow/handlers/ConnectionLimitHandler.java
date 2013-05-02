package org.jboss.as.undertow.handlers;

import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.RequestLimitingHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.undertow.AbstractHandlerDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ConnectionLimitHandler extends AbstractHandlerDefinition {

    private static final AttributeDefinition HIGH_WATER_MARK = new SimpleAttributeDefinitionBuilder("high-water-mark", ModelType.INT)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();
    private static final AttributeDefinition LOW_WATER_MARK = new SimpleAttributeDefinitionBuilder("low-water-mark", ModelType.INT)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

    /*
    <connection-limit high-water-mark="100" low-water-mark="50"/>
     */

    public ConnectionLimitHandler() {
        super("connection-limit");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(HIGH_WATER_MARK, LOW_WATER_MARK);
    }


    public HttpHandler createHandler(HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException {
        return new RequestLimitingHandler(1000, next);
    }
}
