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

package org.jboss.as.server.mgmt;

import org.jboss.as.model.UpdateResultHandler;

/**
 * Support class for invoking the various {@code handleRollbackXXX} methods
 * on an {@link UpdateResultHandler}. Delegates invocations to the {@code UpdateResultHandler}
 * associated with the update that is being rolled back.The
 * {@link org.jboss.as.model.AbstractModelElementUpdate#getCompensatingUpdate(E) compensating update}
 * for an update that is being rolled back invokes the non-{@code handleRollbackXXX} methods
 * on this object, which in turn invokes the {@code handleRollbackXXX} methods
 * on the {@code UpdateResultHandler} associated with the update that is being
 * rolled back.
 * <p>This class avoids a generics problem wherein the result type provided by
 * the compensating update does not match the <R> type of the handler associated
 * with the update being rolled back. This class accepts any result type in
 * {@link #handleSuccess(Object, Object)} since it ignores the result type and
 * just invokes {@link #handleRollbackSuccess(Object)}.</p>
 *
 * @author Brian Stansberry
 */
public class RollbackUpdateResultHandler<P> implements UpdateResultHandler<Object, P> {

    private final UpdateResultHandler<?, P> delegate;

    /** Static factory for RollbackUpdateResultHandler */
    public static <P> RollbackUpdateResultHandler<P> getRollbackUpdateResultHandler(final UpdateResultHandler<?, P> delegate) {
        return new RollbackUpdateResultHandler<P>(delegate);
    }

    private RollbackUpdateResultHandler(final UpdateResultHandler<?, P> delegate) {
        this.delegate = delegate;
    }

    @Override
    public void handleCancellation(P param) {
        delegate.handleRollbackCancellation(param);
    }

    @Override
    public void handleFailure(Throwable cause, P param) {
        delegate.handleRollbackFailure(cause, param);
    }

    @Override
    public void handleSuccess(Object result, P param) {
        delegate.handleRollbackSuccess(param);
    }

    @Override
    public void handleTimeout(P param) {
        delegate.handleRollbackTimeout(param);
    }

    @Override
    public void handleRollbackFailure(Throwable cause, P param) {
        throw new UnsupportedOperationException("handleRollback methods should not be invoked on " + getClass().getSimpleName());
    }

    @Override
    public void handleRollbackSuccess(P param) {
        throw new UnsupportedOperationException("handleRollback methods " +
                "should not be invoked on " + getClass().getSimpleName());
    }

    @Override
    public void handleRollbackCancellation(P param) {
        throw new UnsupportedOperationException("handleRollback methods " +
                "should not be invoked on " + getClass().getSimpleName());
    }

    @Override
    public void handleRollbackTimeout(P param) {
        throw new UnsupportedOperationException("handleRollback methods " +
                "should not be invoked on " + getClass().getSimpleName());
    }
}
