/**
 *
 */
package org.jboss.as.controller;

import org.jboss.as.controller.persistence.ConfigurationPersisterProvider;

/**
 * Provides a context in a which a {@link ModelController} can execute an {@link Operation}
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public interface OperationControllerContext {

    ModelProvider getModelProvider();

    OperationContextFactory getOperationContextFactory();

    ConfigurationPersisterProvider getConfigurationPersisterProvider();

    ControllerTransactionContext getControllerTransactionContext();

    /**
     * Attempts to acquire the controller's exclusive lock, possibly blocking until acquired.
     * If the call acquires the lock, the caller must ensure {@link #unlock()} is called.
     * If the call does not acquire the lock (i.e. this method returns {@code false} or throws an exception)
     * the caller must not attempt to call {@link #unlock()}.
     *
     * @return {@code true} if the call acquired the lock, {@code false} if not. A return
     *         value of {@code false} should not be regarded as an error condition
     *
     * @throws InterruptedException if the thread is interrupted while trying to acquire the lock.
     */
    boolean lockInterruptibly() throws InterruptedException;

    /**
     * Releases any lock acquired in {@link #lockInterruptibly()}.
     */
    void unlock();
}
