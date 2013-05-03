package org.wildfly.extension.undertow.filters;

import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.wildfly.extension.undertow.AbstractHandlerDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ResponseHeaderHandler extends AbstractHandlerDefinition {

    private static final AttributeDefinition NAME = new SimpleAttributeDefinitionBuilder("header-name", ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .build();
    private static final AttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder("header-value", ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .build();

    public ResponseHeaderHandler() {
        super("response-header");
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(NAME, VALUE);
    }

    public HttpHandler createHandler(HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException {
        return new SimpleErrorPageHandler(next);
    }
}
