package org.jboss.as.controller.client;

import java.io.IOException;

/**
 * A handle for a specific running operation.
 */
public interface Cancellable {

    /**
     * Attempt to cancel this operation.
     *
     * @return <tt>false</tt> if the task could not be cancelled;
     * <tt>true</tt> otherwise
     *
     * @throws IOException if an error happened talking to the remote host
     */
    boolean cancel() throws IOException;
}