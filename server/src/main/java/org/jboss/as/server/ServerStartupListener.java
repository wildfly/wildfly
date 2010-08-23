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

package org.jboss.as.server;

import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Service listener used by a starting service to collect the result of all the services being installed.
 *
 * @author John E. Bailey
 */
public class ServerStartupListener extends AbstractServiceListener<Object>{
    private static final Logger log = Logger.getLogger("org.jboss.as.server");
    private volatile int totalServices;
    private volatile int onDemandServices;
    private volatile int startedServices;
    private volatile int count = 1;
    private final long start = System.currentTimeMillis();
    private final Map<ServiceName, StartException> serviceFailures = new HashMap<ServiceName, StartException>();
    private final Callback finishCallback;

    private static final AtomicIntegerFieldUpdater<ServerStartupListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "count");
    private static final AtomicIntegerFieldUpdater<ServerStartupListener> totalServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "totalServices");
    private static final AtomicIntegerFieldUpdater<ServerStartupListener> onDemandServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "onDemandServices");
    private static final AtomicIntegerFieldUpdater<ServerStartupListener> startedServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "startedServices");

    /**
     * Construct new instance with a callback listener.
     *
     * @param finishCallback The finish callback
     */
    public ServerStartupListener(final Callback finishCallback) {
        this.finishCallback = finishCallback;
    }

    /** {@inheritDoc} */
    public void listenerAdded(final ServiceController<? extends Object> serviceController) {
        totalServicesUpdater.incrementAndGet(this);
        if(ServiceController.Mode.ON_DEMAND.equals(serviceController.getMode())) {
            onDemandServicesUpdater.incrementAndGet(this);
        } else {
            countUpdater.incrementAndGet(this);
        }
    }

    /** {@inheritDoc} */
    public void serviceStarted(final ServiceController<? extends Object> serviceController) {
        startedServicesUpdater.incrementAndGet(this);
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
        serviceController.removeListener(this);
    }

    /** {@inheritDoc} */
    public void serviceFailed(ServiceController<? extends Object> serviceController, StartException reason) {
        final ServiceName serviceName = serviceController.getName();
        serviceFailures.put(serviceName, reason);
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
        serviceController.removeListener(this);
        log.errorf(reason, "Service [%s] start failed", serviceName);
    }   

    /**
     * Call when all services in this group have been added.
     */
    public void finishBatch() {
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
    }

    private void batchComplete() {
        final long end = System.currentTimeMillis();
        if(finishCallback != null) {
            finishCallback.run(serviceFailures, end - start, totalServices, onDemandServices, startedServices);
        }
    }

    public static interface Callback {
        void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices);
    }
}