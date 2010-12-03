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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import org.jboss.logging.Logger;
import org.jboss.modules.Module;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;

/**
 * Service listener used by a starting service to collect the result of all the services being installed.
 *
 * @author John E. Bailey
 */
public class ServerStartupListener extends AbstractServiceListener<Object>{
    private static final Logger log = Logger.getLogger("org.jboss.as.server");
    private volatile int totalServices;
    private volatile int startedServices;
    private volatile int startedNonActiveServices;
    private volatile int count = 0;
    private final long start = Module.getStartTime();
    private final Map<ServiceName, StartException> serviceFailures = new HashMap<ServiceName, StartException>();
    private final Callback finishCallback;
    private final Set<ServiceName> expectedNonActiveServices = new HashSet<ServiceName>();
    private final AtomicBoolean callbackRan = new AtomicBoolean();

    private static final AtomicIntegerFieldUpdater<ServerStartupListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "count");
    private static final AtomicIntegerFieldUpdater<ServerStartupListener> totalServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "totalServices");
    private static final AtomicIntegerFieldUpdater<ServerStartupListener> startedServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "startedServices");
    private static final AtomicIntegerFieldUpdater<ServerStartupListener> startedOnDemandServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(ServerStartupListener.class, "startedNonActiveServices");

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
        if(!expectedNonActiveServices.contains(serviceController.getName())) {
            countUpdater.incrementAndGet(this);
        }
    }

    /** {@inheritDoc} */
    public void serviceStarted(final ServiceController<? extends Object> serviceController) {
        startedServicesUpdater.incrementAndGet(this);
        if(expectedNonActiveServices.contains(serviceController.getName())) {
            startedOnDemandServicesUpdater.incrementAndGet(this);
        }
        if (!expectedNonActiveServices.contains(serviceController.getName()) && countUpdater.decrementAndGet(this) == 0) {
            complete();
        }
        serviceController.removeListener(this);
    }

    /** {@inheritDoc} */
    public void serviceFailed(ServiceController<? extends Object> serviceController, StartException reason) {
        final ServiceName serviceName = serviceController.getName();
        log.errorf(reason, "Service [%s] start failed", serviceName);
        serviceFailures.put(serviceName, reason);
        if (!expectedNonActiveServices.contains(serviceController.getName()) && countUpdater.decrementAndGet(this) == 0) {
            complete();
        }
        serviceController.removeListener(this);
    }

    public void finish() {
        if (countUpdater.decrementAndGet(this) == 0) {
            complete();
        }
    }

    private void complete() {
        if(callbackRan.compareAndSet(false, true)) {
            final long end = System.currentTimeMillis();
            finishCallback.run(serviceFailures, end - start, totalServices, expectedNonActiveServices.size() - startedOnDemandServicesUpdater.get(this), startedServices);
        }
    }

    public void expectNonActive(final ServiceName serviceName) {
        expectedNonActiveServices.add(serviceName);
    }

    public void unexpectNonActive(final ServiceName serviceName) {
        expectedNonActiveServices.remove(serviceName);
    }

    public static interface Callback {
        void run(Map<ServiceName, StartException> serviceFailures, long elapsedTime, int totalServices, int onDemandServices, int startedServices);
    }
}
