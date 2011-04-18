/**
 *
 */
package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;

/**
 * Transactional context in which an operation is executed by a {@link ModelController}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface ControllerTransactionContext {

    /**
     * Gets the id of the transaction.
     *
     * @return the id. Will not be {@link null}
     */
    ModelNode getTransactionId();

    /**
     * Register a resource with the transaction.
     *
     * @param resource the resource
     */
    void registerResource(ControllerResource resource);

    /**
     * Deregister a previously registered resource from the transaction.
     *
     * @param resource the resource
     */
    void deregisterResource(ControllerResource resource);

    /**
     * Mark the transaction rollback only
     */
    void setRollbackOnly();

    /**
     * Register a listener for notifications before and after transaction commit.
     *
     * @param synchronization the listener
     */
    void registerSynchronization(ControllerTransactionSynchronization synchronization);
}
