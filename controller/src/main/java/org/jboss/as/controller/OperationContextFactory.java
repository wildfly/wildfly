/**
 *
 */
package org.jboss.as.controller;


/**
 * Factory for an {@link OperationContext}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface OperationContextFactory {

    OperationContext getOperationContext(final ModelProvider modelSource, final PathAddress address, final OperationHandler operationHandler);
}
