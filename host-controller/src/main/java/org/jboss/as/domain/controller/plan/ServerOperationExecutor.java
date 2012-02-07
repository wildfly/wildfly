/**
 *
 */
package org.jboss.as.domain.controller.plan;

import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * Callback from a task when it wants to execute an operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ServerOperationExecutor {

    /**
     * Execute an operation against the given server
     * @param server  the identity of the server
     * @param operation the operation
     * @return the result, or {@code null} if the server is unknown
     */
    ModelNode executeServerOperation(ServerIdentity server, ModelNode operation);
}
