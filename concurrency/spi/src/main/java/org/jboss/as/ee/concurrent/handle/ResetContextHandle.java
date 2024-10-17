/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

/**
 * The Wildfly's EE reset context handle.
 *
 * @author Eduardo Martins
 */
public interface ResetContextHandle extends ContextHandle {

    /**
     * Called by ManagedExecutorService after executing a task to clean up and reset thread context. It will be called in the thread that was used for executing the task.
     */
    void reset();

    /**
     * Retrieves the name of the factory which built the handle.
     * @return
     */
    String getFactoryName();
}
