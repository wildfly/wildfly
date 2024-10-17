/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

/**
 * The Wildfly's EE context handle that sets a saved invocation context.
 *
 * @author Eduardo Martins
 */
public interface SetupContextHandle extends ContextHandle {

    /**
     * Called by ManagedExecutorService before executing a task to set up thread context. It will be called in the thread that will be used for executing the task.
     * @return A ContextHandle that will be passed to the reset method in the thread executing the task
     * @throws IllegalStateException if the ContextHandle is no longer valid. For example, the application component that the ContextHandle was created for is no longer running or is undeployed.
     */
    ResetContextHandle setup() throws IllegalStateException;

    /**
     * Retrieves the name of the factory which built the handle.
     * @return
     */
    String getFactoryName();
}
