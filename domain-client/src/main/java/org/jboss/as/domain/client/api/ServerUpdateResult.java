package org.jboss.as.domain.client.api;

import java.util.UUID;

/**
 * Encapsulates the results of performing a configuration modification on an individual
 * server.
 *
 * @param R the type of the object returned by the update when it was
 *          applied to the runtime
 *
 * @author Brian Stansberry
 */
public interface ServerUpdateResult<R> {

    /**
     * Gets the unique ID of the deployment action.
     *
     * @return the ID. Will not be <code>null</code>
     */
    UUID getUpdateActionId();

    /**
     * Gets the id of the server on which this update was executed.
     *
     * @return the server identity. Will not be <code>null</code>
     */
    ServerIdentity getServerIdentity();

    /**
     * Gets the result of the action's modification to the server's configuration.
     * This will always be {@code null} if {@link #isServerRestarted()} is <code>true</code>.
     *
     * @return the result. May be <code>null</code>
     */
    R getSuccessResult();

    /**
     * Gets the exception, if any, that occurred while executing this update.
     *
     * @return the exception, or <code>null</code> if no exception occurred
     */
    Throwable getFailureResult();

    /**
     * Gets whether the application of this action on this server was
     * cancelled.
     *
     * @return <code>true</code> if the action was cancelled; <code>false</code>
     *         otherwise
     */
    boolean isCancelled();

    /**
     * Gets whether the application of this action on this server timed out.
     *
     * @return <code>true</code> if the action timed out; <code>false</code>
     *         otherwise
     */
    boolean isTimedOut();

    /**
     * Gets whether the application of this action on this server was
     * rolled back.
     *
     * @return <code>true</code> if the action was rolled back; <code>false</code>
     *         otherwise
     */
    boolean isRolledBack();

    /**
     * Gets whether the application of the update to the server's runtime
     * required a server restart.
     *
     * @return <code>true</code> if the server was restarted; <code>false</code> otherwise
     */
    boolean isServerRestarted();

}
