package org.jboss.as.undertow.handlers;

import io.undertow.security.handlers.AuthenticationCallHandler;
import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.undertow.AbstractHandlerResourceDefinition;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class BasicAuthHandler extends AbstractHandlerResourceDefinition {

    private static SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinitionBuilder("security-domain", ModelType.STRING)
            .setAllowNull(false)
            .build();

    public BasicAuthHandler() {
        super("basic-auth");
    }

    @Override
    public AttributeDefinition[] getAttributes() {
        return new AttributeDefinition[]{SECURITY_DOMAIN};
    }

    @Override
    public HttpHandler createHandler(HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException {
        return new AuthenticationCallHandler(next);
    }
}
