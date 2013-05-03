package org.wildfly.extension.undertow;

import io.undertow.server.HttpHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.dmr.ModelNode;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2013 Red Hat Inc.
 */
public interface Handler extends PersistentResourceDefinition {

    String getName();

    HttpHandler createHandler(final HttpHandler next, OperationContext context, ModelNode model) throws OperationFailedException;

}
