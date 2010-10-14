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

/**
 * Encapsulates the possible values that can be passed to an
 * {@link UpdateResultHandler}'s callback methods.
 *
 * @author Brian Stansberry
 */
public class UpdateResultHandlerResponse<R> {

    private final R successResult;
    private final Throwable failureResult;
    private final boolean cancelled;
    private final boolean timedOut;

    public static <R> UpdateResultHandlerResponse<R> createSuccessResponse(R result) {
        return new UpdateResultHandlerResponse<R>(result, null, false, false);
    }

    public static <R> UpdateResultHandlerResponse<R> createFailureResponse(Throwable cause) {
        return new UpdateResultHandlerResponse<R>(null, cause, false, false);
    }

    public static <R> UpdateResultHandlerResponse<R> createCancellationResponse() {
        return new UpdateResultHandlerResponse<R>(null, null, true, false);
    }

    public static <R> UpdateResultHandlerResponse<R> createTimeoutResponse() {
        return new UpdateResultHandlerResponse<R>(null, null, false, true);
    }

    private UpdateResultHandlerResponse(final R successResult, final Throwable failureResult, final boolean cancelled, final boolean timedOut) {
        this.successResult = successResult;
        this.failureResult = failureResult;
        this.cancelled = cancelled;
        this.timedOut = timedOut;
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

}
