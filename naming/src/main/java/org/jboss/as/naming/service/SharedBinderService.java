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
 * A binder service which "ownership" may be shared.
 * Unless forced, the shared binder service only stops after all "owners" release it.
 *
 * @author Eduardo Martins
 */
public class SharedBinderService extends BinderService {

    /**
     * the context of a suspended stop
     */
    private StopContext stopContext;

    /**
     * the owners of the service
     */
    private Owners owners = new Owners();

    /**
     *
     * @param name
     * @param source
     */
    public SharedBinderService(final String name, Object source) {
        super(name, source);
    }

    /**
     *
     * @param name
     */
    public SharedBinderService(final String name) {
        this(name, null);
    }

    /**
     * Retrieves the bind's owners
     * @return
     */
    public Owners getOwners() {
        return owners;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);
    }

    @Override
    public synchronized void stop(StopContext context) {
        if (!owners.isOwned()) {
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
        if (owners.isOwned()) {
            owners.releaseAll();
        } else {
            noOwners();
        }
    }

    /**
     * Proceeds with the service stop.
     * @param stopContext
     */
    private void proceedWithStop(StopContext stopContext) {
        super.stop(stopContext);
        // if service stop was forced then there may still be references to current owners instance, recreate it to allow clean service restart
        owners = new Owners();
    }

    /**
     * The bind has no more owners, if the bind is up then the binder service is stopped.
     */
    private void noOwners() {
        if (stopContext != null) {
            // suspended stop, resume it
            proceedWithStop(stopContext);
            stopContext.complete();
            stopContext = null;
        } else if (controller != null) {
            // service is up, stop it
            controller.setMode(ServiceController.Mode.REMOVE);
        }
    }

    @Override
    public String toString() {
        return new StringBuilder("SharedBinderService[name=").append(name).append(", source=").append(source).append(", owners=").append(owners.ownersCount).append(']').toString();
    }

    /**
     * The owner's of the service.
     */
    public class Owners {

        private int ownersCount = 0;

        public synchronized boolean isOwned() {
            return ownersCount > 0;
        }

        public synchronized void acquire() {
            ownersCount++;
        }

        /**
         * An owner releases the binder service.
         */
        public synchronized void release() {
            if (isOwned()) {
                ownersCount--;
                if (!isOwned()) {
                    noOwners();
                }
            }
        }

        /**
         * The binder service invalidates all ownerships.
         */
        private synchronized void releaseAll() {
            if (isOwned()) {
                ownersCount = 0;
                noOwners();
            }
        }

        /**
         * Retrieves the service name of the binder service.
         * @return
         */
        public ServiceName getServiceName() {
            final ServiceController<?> serviceController = controller;
            return serviceController != null ? serviceController.getName() : null;
        }
    }
}
