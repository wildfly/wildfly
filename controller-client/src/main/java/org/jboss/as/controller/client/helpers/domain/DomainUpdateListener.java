/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.controller.client.helpers.domain;

import java.util.Collection;
import java.util.Map;

/**
 * Callback interface for listeners that wish to receive notifications related
 * to the execution of an update to the domain. Besides the domain-level notifications
 * specified in this {@link UpdateResultHandler} sub-interface, implementations
 * will also receive {@link UpdateResultHandler} notifications for every server to
 * which the update is applied.
 *
 * @author Brian Stansberry
 */
public interface DomainUpdateListener<R> {

    /**
     * Handle successful application of the update.
     *
     * @param result the update result, if any
     * @param the server that generated the event
     */
    void handleSuccess(R result, ServerIdentity server);

    /**
     * Handle a failure to apply the update.
     *
     * @param cause the cause of the failure
     * @param the server that generated the event
     */
    void handleFailure(Throwable cause, ServerIdentity server);

    /**
     * Handle cancellation of the update.
     *
     * @param the server that generated the event
     */
    void handleCancellation(ServerIdentity server);

    /**
     * Handle a timeout in applying the update.
     *
     * @param the server that generated the event
     */
    void handleTimeout(ServerIdentity server);

    /**
     * Handle a successful rollback of the update.
     *
     * @param the server that generated the event
     */
    void handleRollbackSuccess(ServerIdentity server);

    /**
     * Handle a failed rollback of the update.
     *
     * @param cause the cause of the failure
     * @param the server that generated the event
     */
    void handleRollbackFailure(Throwable cause, ServerIdentity server);

    /**
     * Handle cancellation of a rollback of the update.
     *
     * @param the server that generated the event
     */
    void handleRollbackCancellation(ServerIdentity server);

    /**
     * Handle a timeout in rolling back the update.
     *
     * @param the server that generated the event
     */
    void handleRollbackTimeout(ServerIdentity server);

    /**
     * Handle the event of the update failing to apply to the domain.
     *
     * @param reason the reason for the failure
     */
    void handleDomainFailed(UpdateFailedException reason);

    /**
     * Handle the event of the update failing to apply to one or more host controllers.
     *
     * @param hostFailureReasons a map of host name to failure cause
     */
    void handleHostFailed(Map<String, UpdateFailedException> hostFailureReasons);

    /**
     * Handle the event of the update successfully applying to the domain and to applicable host
     * controllers.
     *
     * @param affectedServers the servers to which the update will be applied (resulting in
     *  subsequent invocations on the methods in the {@link UpdateResultHandler super-interface}
     */
    void handleServersIdentified(Collection<ServerIdentity> affectedServers);

    /**
     * Handle the event of the execution of the update being cancelled.
     * This would occur as a result of a previously executed update in the same set of updates
     * failing to apply successfully to the domain.
     */
    void handleCancelledByDomain();

    /**
     * Handle the event of the execution of the update being rolled back
     * after it was successfully applied to the domain and to the host controllers.
     * This would occur as a result of another update in the same set of updates
     * failing to apply successfully.
     */
    void handleDomainRollback();

    /**
     * Handle the event of the rollback of the update failing to apply to the domain.
     *
     * @param reason the reason for the failure
     */
    void handleDomainRollbackFailed(UpdateFailedException reason);

    /**
     * Handle the event of the rollback of the update failing to apply to one
     * or more host controllers.
     *
     * @param hostFailureReasons a map of host name to failure cause
     */
    void handleHostRollbackFailed(Map<String, UpdateFailedException> hostFailureReasons);

    /**
     * Handle the final completion of the update, after which the update is no
     * longer eligible to be rolled back. This notification will be
     * emitted for every update, even if the update failed or was {@link #handleCancelled}.
     */
    void handleComplete();

}
