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
package org.jboss.as.server;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.as.version.Version;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class BootstrapListener extends AbstractServiceListener<Object> {

    private final AtomicInteger started = new AtomicInteger();
    private final AtomicInteger failed = new AtomicInteger();
    private final AtomicInteger outstanding = new AtomicInteger(1);
    private final AtomicBoolean done = new AtomicBoolean();
    private final AtomicInteger missingDeps = new AtomicInteger();
    private final EnumMap<ServiceController.Mode, AtomicInteger> map;
    private final ServiceContainer serviceContainer;
    private final Set<ServiceName> missingDepsSet = Collections.synchronizedSet(new TreeSet<ServiceName>());
    private final ServiceTarget serviceTarget;
    private final long startTime;
    private volatile boolean cancelLikely;

    private final FutureServiceContainer futureContainer;
    private final String prettyVersion;

    public BootstrapListener(final ServiceContainer serviceContainer, final long startTime, final ServiceTarget serviceTarget, final FutureServiceContainer futureContainer, final String prettyVersion) {
        this.serviceContainer = serviceContainer;
        this.startTime = startTime;
        this.serviceTarget = serviceTarget;
        this.futureContainer = futureContainer;
        this.prettyVersion = prettyVersion;
        final EnumMap<ServiceController.Mode, AtomicInteger> map = new EnumMap<ServiceController.Mode, AtomicInteger>(ServiceController.Mode.class);
        for (ServiceController.Mode mode : ServiceController.Mode.values()) {
            map.put(mode, new AtomicInteger());
        }
        this.map = map;
    }

    @Override
    public void listenerAdded(final ServiceController<?> controller) {
        final ServiceController.Mode mode = controller.getMode();
        if (mode == ServiceController.Mode.ACTIVE) {
            outstanding.incrementAndGet();
        } else {
            controller.removeListener(this);
        }
        map.get(mode).incrementAndGet();
    }

    @Override
    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
        switch (transition) {
            case STARTING_to_UP: {
                started.incrementAndGet();
                controller.removeListener(this);
                tick();
                break;
            }
            case STARTING_to_START_FAILED: {
                failed.incrementAndGet();
                controller.removeListener(this);
                tick();
                break;
            }
            case START_REQUESTED_to_PROBLEM: {
                missingDeps.incrementAndGet();
                check();
                break;
            }
            case PROBLEM_to_START_REQUESTED: {
                missingDeps.decrementAndGet();
                check();
                break;
            }
            case REMOVING_to_REMOVED: {
                cancelLikely = true;
                tick();
                break;
            }
        }
    }

    private void check() {
        int outstanding = this.outstanding.get();
        if (outstanding == missingDeps.get()) {
            finish(serviceContainer, outstanding);
        }
    }

    public void tick() {
        int outstanding = this.outstanding.decrementAndGet();
        if (outstanding != missingDeps.get()) {
            return;
        }
        finish(serviceContainer, outstanding);
    }

    private void finish(final ServiceContainer container, final int outstanding) {
        if (done.getAndSet(true)) {
            return;
        }
        serviceTarget.removeListener(this);
        if (cancelLikely) {
            return;
        }

        final int failed = this.failed.get() + outstanding;
        final long elapsedTime = Math.max(System.currentTimeMillis() - startTime, 0L);
        final int started = this.started.get();
        done(container, elapsedTime, started, failed, map, missingDepsSet);
    }

    protected void done(ServiceContainer container, long elapsedTime, int started, int failed, EnumMap<ServiceController.Mode, AtomicInteger> map, Set<ServiceName> missingDepsSet) {
        futureContainer.done(container);
        final Logger log = Logger.getLogger("org.jboss.as");
        final int active = map.get(ServiceController.Mode.ACTIVE).get();
        final int passive = map.get(ServiceController.Mode.PASSIVE).get();
        final int onDemand = map.get(ServiceController.Mode.ON_DEMAND).get();
        final int never = map.get(ServiceController.Mode.NEVER).get();
        if (failed == 0) {
            log.infof("%s started in %dms - Started %d of %d services (%d services are passive or on-demand)", prettyVersion, Long.valueOf(elapsedTime), Integer.valueOf(started), Integer.valueOf(active + passive + onDemand + never), Integer.valueOf(onDemand + passive));
        } else {
            log.errorf("%s started (with errors) in %dms - Started %d of %d services (%d services failed or missing dependencies, %d services are passive or on-demand)", prettyVersion, Long.valueOf(elapsedTime), Integer.valueOf(started), Integer.valueOf(active + passive + onDemand + never), Integer.valueOf(failed), Integer.valueOf(onDemand + passive));
        }
    }
}
