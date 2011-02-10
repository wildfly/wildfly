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

package org.jboss.as.domain.client.impl;

import java.io.Serializable;

/**
 * Encapsulates the possible values that can be passed to an
 * {@link UpdateResultHandler}'s callback methods.
 *
 * @author Brian Stansberry
 */
public class UpdateResultHandlerResponse<R> implements Serializable {

    private static final long serialVersionUID = -5250735019112151634L;

    private final R successResult;
    private final Throwable failureResult;
    private final boolean cancelled;
    private final boolean timedOut;
    private final boolean rolledBack;
    private final boolean restarted;
    private final boolean rollbackCancelled;
    private final boolean rollbackTimedOut;
    private final Throwable rollbackFailure;

    public static <R> UpdateResultHandlerResponse<R> createSuccessResponse(R result) {
        return new UpdateResultHandlerResponse<R>(result, null, false, false, false, false, false, false, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createFailureResponse(Throwable cause) {
        return new UpdateResultHandlerResponse<R>(null, cause, false, false, false, false, false, false, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createCancellationResponse() {
        return new UpdateResultHandlerResponse<R>(null, null, true, false, false, false, false, false, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createTimeoutResponse() {
        return new UpdateResultHandlerResponse<R>(null, null, false, true, false, false, false, false, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createRollbackResponse(UpdateResultHandlerResponse<R> rolledBack) {
        return new UpdateResultHandlerResponse<R>(rolledBack.successResult, rolledBack.failureResult,
                rolledBack.cancelled, rolledBack.timedOut, rolledBack.restarted, true, false, false, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createRollbackCancelledResponse(UpdateResultHandlerResponse<R> rolledBack) {
        return new UpdateResultHandlerResponse<R>(rolledBack.successResult, rolledBack.failureResult,
                rolledBack.cancelled, rolledBack.timedOut, rolledBack.restarted, false, true, false, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createRollbackTimedOutResponse(UpdateResultHandlerResponse<R> rolledBack) {
        return new UpdateResultHandlerResponse<R>(rolledBack.successResult, rolledBack.failureResult,
                rolledBack.cancelled, rolledBack.timedOut, rolledBack.restarted, false, false, true, null);
    }

    public static <R> UpdateResultHandlerResponse<R> createRollbackFailedResponse(UpdateResultHandlerResponse<R> rolledBack, Throwable cause) {
        return new UpdateResultHandlerResponse<R>(rolledBack.successResult, rolledBack.failureResult,
                rolledBack.cancelled, rolledBack.timedOut, rolledBack.restarted, false, false, false, cause);
    }

    public static <R> UpdateResultHandlerResponse<R> createRestartResponse() {
        return new UpdateResultHandlerResponse<R>(null, null, false, false, true, false, false, false, null);
    }

    private UpdateResultHandlerResponse(final R successResult, final Throwable failureResult,
            final boolean cancelled, final boolean timedOut,
            final boolean restarted, final boolean rolledBack,
            final boolean rollbackCancelled, final boolean rollbackTimedOut,
            final Throwable rollbackFailure) {
        this.successResult = successResult;
        this.failureResult = failureResult;
        this.cancelled = cancelled;
        this.timedOut = timedOut;
        this.restarted = restarted;
        this.rolledBack = rolledBack;
        this.rollbackCancelled = rollbackCancelled;
        this.rollbackTimedOut = rollbackTimedOut;
        this.rollbackFailure = rollbackFailure;
    }


    public boolean isRollbackCancelled() {
        return rollbackCancelled;
    }

    public boolean isRollbackTimedOut() {
        return rollbackTimedOut;
    }

    public Throwable getRollbackFailure() {
        return rollbackFailure;
    }

    public R getSuccessResult() {
        return successResult;
    }

    public Throwable getFailureResult() {
        return failureResult;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isTimedOut() {
        return timedOut;
    }

    public boolean isRolledBack() {
        return rolledBack;
    }

    public boolean isServerRestarted() {
        return restarted;
    }

}
