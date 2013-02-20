package org.jboss.as.undertow.handlers;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.undertow.AbstractHandlerResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public class SimpleErrorPageHandler extends AbstractHandlerResourceDefinition {

    public SimpleErrorPageHandler() {
        super("simple-error-page");
    }

    @Override
    public HttpHandler createHandler(HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException {
        return new io.undertow.server.handlers.error.SimpleErrorPageHandler(next);
    }
}
