/**
 *
 */
package org.jboss.as.controller;

import org.jboss.as.controller.client.ExecutionContext;


/**
 * Factory for an {@link OperationContext}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface OperationContextFactory {

    OperationContext getOperationContext(final ModelProvider modelSource, final PathAddress address, final OperationHandler operationHandler, final ExecutionContext executionContext);
}
