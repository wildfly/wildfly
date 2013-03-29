package org.jboss.as.undertow.handlers;

import java.util.Arrays;
import java.util.Collection;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.error.SimpleErrorPageHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.undertow.AbstractHandlerResourceDefinition;
import org.jboss.as.undertow.Constants;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class ResponseHeaderHandler extends AbstractHandlerResourceDefinition {

    private static final AttributeDefinition NAME = new SimpleAttributeDefinitionBuilder(Constants.NAME, ModelType.STRING)
            .setAllowNull(false)
            .setAllowExpression(true)
            .build();
    private static final AttributeDefinition VALUE = new SimpleAttributeDefinitionBuilder("value", ModelType.STRING)
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

    @Override
    public HttpHandler createHandler(HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException {
        return new SimpleErrorPageHandler(next);
    }
}
