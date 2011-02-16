/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.controller;

import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ResultHandler {

    /**
     * Add a result fragment to the final result.
     *
     * @param location the location of the fragment within the final result
     * @param result the result fragment to insert
     */
    void handleResultFragment(String[] location, ModelNode result);

    /**
     * Handle operation completion.
     */
    void handleResultComplete();

    /**
     * Handle an operation failure.
     *
     * @param failureDescription the failure description
     */
    void handleFailed(ModelNode failureDescription);

    /**
     * Signify that this operation was cancelled.
     */
    void handleCancellation();

    /**
     * A listener which invokes a {@link ResultHandler} when the service it is attached to has been removed.
     *
     */
    class ServiceRemoveListener extends AbstractServiceListener<Object> {
        private final ResultHandler resultHandler;

        public ServiceRemoveListener(final ResultHandler resultHandler) {
            this.resultHandler = resultHandler;
        }

        @Override
        public void listenerAdded(final ServiceController<?> controller) {
            controller.setMode(ServiceController.Mode.REMOVE);
        }

        @Override
        public void serviceRemoved(final ServiceController<?> controller) {
            resultHandler.handleResultComplete();
        }
    }

    /**
     * A listener which invokes a {@link ResultHandler} when the service it is attached to has been added and
     * started successfully.
     *
     */
    class ServiceStartListener extends AbstractServiceListener<Object> {
        private final ResultHandler resultHandler;

        public ServiceStartListener(final ResultHandler resultHandler) {
            this.resultHandler = resultHandler;
        }

        @Override
        public void serviceStarted(final ServiceController<?> controller) {
            try {
                resultHandler.handleResultComplete();
            } finally {
                controller.removeListener(this);
            }
        }

        @Override
        public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
            try {
                resultHandler.handleFailed(new ModelNode().set(reason.getLocalizedMessage()));
            } finally {
                controller.removeListener(this);
            }
        }

        @Override
        public void serviceRemoved(final ServiceController<?> controller) {
            try {
                resultHandler.handleCancellation();
            } finally {
                controller.removeListener(this);
            }
        }
    }
}
