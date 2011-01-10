package org.jboss.as.controller.client;

import java.io.IOException;

/**
 * A handle for a specific running operation.
 */
public interface Cancellable {

    /**
     * Attempt to cancel this operation.
     * @throws IOException if an error happened talking to the remote host
     */
    void cancel() throws IOException;
}