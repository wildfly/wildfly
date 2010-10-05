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

package org.jboss.as.model;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartException;

/**
 * The result of applying an update to a running server.
 *
 * @param <P> the type of the parameter to pass to the handler instance
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface UpdateResultHandler<R, P> {

    /**
     * Handle successful application of the update.
     *
     * @param result the update result, if any
     * @param param the parameter passed in to the update method
     */
    void handleSuccess(R result, P param);

    /**
     * Handle a failure to apply the update.
     *
     * @param cause the cause of the failure
     * @param param the parameter passed in to the update method
     */
    void handleFailure(Throwable cause, P param);

    /**
     * Handle cancellation of the update.
     *
     * @param param the parameter passed in to the update method
     */
    void handleCancellation(P param);

    /**
     * Handle a timeout in applying the update.
     *
     * @param param the parameter passed in to the update method
     */
    void handleTimeout(P param);

    /**
     * Handle a successful rollback of the update.
     *
     * @param param the parameter passed in to the update method
     */
    void handleRollbackSuccess(P param);

    /**
     * Handle a failed rollback of the update.
     *
     * @param cause the cause of the failure
     * @param param the parameter passed in to the update method
     */
    void handleRollbackFailure(Throwable cause, P param);

    void handleRollbackCancellation(P param);

    void handleRollbackTimeout(P param);

    /**
     * An update result handler which does nothing.
     */
    UpdateResultHandler<Object, Void> NULL = new UpdateResultHandler<Object, Void>() {
        public void handleSuccess(final Object result, final Void param) {
        }

        public void handleFailure(final Throwable cause, final Void param) {
        }

        public void handleTimeout(final Void param) {
        }

        public void handleCancellation(final Void param) {
        }

        public void handleRollbackSuccess(final Void param) {
        }

        public void handleRollbackFailure(final Throwable cause, final Void param) {
        }

        public void handleRollbackCancellation(final Void param) {
        }

        public void handleRollbackTimeout(final Void param) {
        }
    };

    /**
     * A listener which invokes an {@link UpdateResultHandler} when the service it is attached to has been removed.
     *
     * @param <P> the update result handler parameter type
     */
    class ServiceRemoveListener<P> extends AbstractServiceListener<Object> {
        private final UpdateResultHandler<?, P> resultHandler;
        private final P param;

        public ServiceRemoveListener(final UpdateResultHandler<?, P> resultHandler, final P param) {
            this.resultHandler = resultHandler;
            this.param = param;
        }

        public void listenerAdded(final ServiceController<?> controller) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }

        public void serviceRemoved(final ServiceController<?> controller) {
            resultHandler.handleSuccess(null, param);
        }
    }

    /**
     * A listener which invokes an {@link UpdateResultHandler} when the service it is attached to has been added and
     * started successfully.
     *
     * @param <P> the update result handler parameter type
     */
    class ServiceStartListener<P> extends AbstractServiceListener<Object> {
        private final UpdateResultHandler<?, P> resultHandler;
        private final P param;

        public ServiceStartListener(final UpdateResultHandler<?, P> resultHandler, final P param) {
            this.resultHandler = resultHandler;
            this.param = param;
        }

        public void serviceStarted(final ServiceController<?> controller) {
            try {
                resultHandler.handleSuccess(null, param);
            } finally {
                controller.removeListener(this);
            }
        }

        public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
            try {
                resultHandler.handleFailure(reason, param);
            } finally {
                controller.removeListener(this);
            }
        }

        public void serviceRemoved(final ServiceController<?> controller) {
            try {
                resultHandler.handleCancellation(param);
            } finally {
                controller.removeListener(this);
            }
        }
    }
}
