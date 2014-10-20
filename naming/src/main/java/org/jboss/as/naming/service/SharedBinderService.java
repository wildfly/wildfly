/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.naming.service;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A binder service which "ownership" may be shared. Unless forced, the shared binder service only stops after all "owners" release it.
 *
 * Owners explicitly "acquire" the service, receiving in return a handler which should be used to "release" the ownership.
 *
 * @author Eduardo Martins
 */
public class SharedBinderService extends BinderService {

    private int owners = 0;
    private StopContext stopContext;
    private ReleaseHandler releaseHandler = new ReleaseHandler(this);

    /**
     * Construct new instance.
     *
     * @param name The JNDI name to use for binding. May be either an absolute or relative name
     * @param source
     */
    public SharedBinderService(final String name, Object source) {
        super(name, source);
    }

    public SharedBinderService(final String name) {
        this(name, null);
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);
    }

    @Override
    public synchronized void stop(StopContext context) {
        // the service is stopping
        if (owners < 1) {
            // no owners, proceed with the service stop
            proceedWithStop(context);
        } else {
            // there are still owners, suspend the service stop
            stopContext = context;
            stopContext.asynchronous();
        }
    }

    @Override
    public synchronized void stopNow() {
        // force stop by releasing all owners
        while (owners > 0) {
            release();
        }
    }

    /**
     * Proceeds with the service stop.
     * @param stopContext
     */
    private void proceedWithStop(StopContext stopContext) {
        super.stop(stopContext);
        if (releaseHandler != null) {
            releaseHandler.invalidate();
        }
        releaseHandler = new ReleaseHandler(this);
        owners = 0;
    }

    /**
     * Acquires the shared bind.
     * @return the handler to use when releasing the ownership
     */
    public synchronized ReleaseHandler acquire() {
        owners++;
        return releaseHandler;
    }

    /**
     * An owner releases the shared bind. If there are no further owners, and the bind is up, then the binder service is stopped.
     */
    private synchronized void release() {
        owners--;
        if (owners < 1) {
            // no more refs, if started stop the service
            if (stopContext != null) {
                // suspended stop, resume it
                proceedWithStop(stopContext);
                stopContext.complete();
                stopContext = null;
            } else if (controller != null) {
                // trigger the service stop
                controller.setMode(ServiceController.Mode.REMOVE);
            }
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("SharedBinderService[name=").append(name).append(", source=").append(source).append(", owners=").append(owners).append(']').toString();
    }

    /**
     * The owner's handler to release a shared binder service ownership.
     */
    public static class ReleaseHandler {

        private final SharedBinderService binderService;
        private boolean invalidated;

        private ReleaseHandler(SharedBinderService binderService) {
            this.binderService = binderService;
        }

        /**
         * An owner releases the binder service.
         */
        public synchronized void release() {
            if (!invalidated) {
                binderService.release();
            }
        }

        /**
         * The binder service invalidates all ownerships.
         */
        private synchronized void invalidate() {
            invalidated = true;
        }

        /**
         * Retrieves the service name of the binder service.
         * @return
         */
        public ServiceName getServiceName() {
            final ServiceController<?> controller = binderService.controller;
            return controller != null ? controller.getName() : null;
        }
    }
}
