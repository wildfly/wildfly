/**
 *
 */
package org.jboss.as.domain.controller.plan;

import org.jboss.as.controller.client.Operation;
import org.jboss.as.domain.controller.ServerIdentity;
import org.jboss.dmr.ModelNode;

/**
 * Callback from a task when it wants to execute an operation.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ServerOperationExecutor {

    ModelNode executeServerOperation(ServerIdentity server, Operation operation);
}
