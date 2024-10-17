/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.ee.concurrent.handle;

import jakarta.enterprise.concurrent.ContextService;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

/**
 * The factory responsible for creating the context handles with the current context saved
 *
 * @author Eduardo Martins
 */
public interface ContextHandleFactory {

    /**
     * Called by ManagedExecutorService in the same thread that submits a
     * task to save the execution context of the submitting thread.
     *
     * @param contextService ContextService containing information on what
     * context should be saved
     * @param contextObjectProperties Additional properties specified for
     * for a context object when the ContextService object was created.
     *
     * @return A ContextHandle that will be passed to the setup method
     * in the thread executing the task
     */
    SetupContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties);

    /**
     * The factory priority is used to define the order of handles when chained. The handle with the lowest priority is the first in the chain.
     * @return
     */
    int getChainPriority();

    /**
     * Retrieves the factory's name.
     * @return
     */
    String getName();

    /**
     * Writes the handle to the specified output stream.
     * @param contextHandle
     * @param out
     */
    void writeSetupContextHandle(SetupContextHandle contextHandle, ObjectOutputStream out) throws IOException;

    /**
     * Reads a handle from the specified input stream.
     * @param in
     * @return
     */
    SetupContextHandle readSetupContextHandle(ObjectInputStream in) throws IOException, ClassNotFoundException;

}
