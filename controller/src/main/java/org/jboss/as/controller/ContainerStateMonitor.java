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

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ContainerStateMonitor extends AbstractServiceListener<Object> {

    private static final Logger log = Logger.getLogger("org.jboss.as.controller");

    private final ServiceRegistry serviceRegistry;
    private final ServiceController<?> controllerController;
    private final AtomicInteger busyServiceCount = new AtomicInteger();

    // protected by "this"
    /** Failed controllers pending tick reaching zero */
    private final Map<ServiceController<?>, String> failedControllers = new IdentityHashMap<ServiceController<?>, String>();
    /** Failed controllers as of the last time tick reached zero */
    private final Map<ServiceController<?>, String> latestSettledFailedControllers = new IdentityHashMap<ServiceController<?>, String>();
    /** Failed controllers as of the last time getServerStateChangeReport() was called */
    private final Map<ServiceController<?>, String> lastReportFailedControllers = new IdentityHashMap<ServiceController<?>, String>();
    /** Services with missing deps */
    private final Set<ServiceController<?>> servicesWithMissingDeps = identitySet();
    /** Services with missing deps as of the last time tick reached zero */
    private Set<ServiceName> previousMissingDepSet = new HashSet<ServiceName>();
    /** Services with missing deps as of the last time getServerStateChangeReport() was called */
    private final Set<ServiceName> lastReportMissingDepSet = new TreeSet<ServiceName>();

    ContainerStateMonitor(final ServiceRegistry registry, final ServiceController<?> controller) {
        serviceRegistry = registry;
        controllerController = controller;
    }

    void acquire() {
        untick();
    }

    void release() {
        tick();
    }

    @Override
    public void listenerAdded(final ServiceController<?> controller) {
        if (controller == controllerController) {
            controller.removeListener(this);
        } else {
            untick();
        }
    }

    @Override
    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
        switch (transition) {
            case STARTING_to_START_FAILED: {
                synchronized (this) {
                    failedControllers.put(controller, controller.getStartException().toString());
                }
                break;
            }
            case REMOVING_to_REMOVED: {
                synchronized (this) {
                    failedControllers.remove(controller);
                    servicesWithMissingDeps.remove(controller);
                }
                break;
            }
            case START_FAILED_to_DOWN:
            case START_FAILED_to_STARTING: {
                synchronized (this) {
                    failedControllers.remove(controller);
                }
                break;
            }
        }
        final ServiceController.Substate before = transition.getBefore();
        final ServiceController.Substate after = transition.getAfter();
        if (before.isRestState() && ! after.isRestState()) {
            untick();
        } else if (! before.isRestState() && after.isRestState()) {
            tick();
        }
    }

    @Override
    public void immediateDependencyAvailable(final ServiceController<?> controller) {
        synchronized (this) {
            servicesWithMissingDeps.remove(controller);
        }
    }

    @Override
    public void immediateDependencyUnavailable(final ServiceController<?> controller) {
        synchronized (this) {
            servicesWithMissingDeps.add(controller);
        }
    }

    void awaitUninterruptibly() {
        awaitUninterruptibly(0);
    }

    void awaitUninterruptibly(int count) {
        boolean intr = false;
        try {
            synchronized (this) {
                while (busyServiceCount.get() > count) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        intr = true;
                    }
                }
            }
        } finally {
            if (intr) {
                Thread.currentThread().interrupt();
            }
        }
    }

    void await() throws InterruptedException {
        await(0);
    }

    void await(int count) throws InterruptedException {
        synchronized (this) {
            while (busyServiceCount.get() > count) {
                wait();
            }
        }
    }

    /**
     * Tick down the count, triggering a deployment status report when the count is zero.
     */
    private void tick() {
        int tick = busyServiceCount.decrementAndGet();

        synchronized (this) {
            notifyAll();
            if (tick == 0) {
                final Set<ServiceName> missingDeps = new HashSet<ServiceName>();
                for (ServiceController<?> controller : servicesWithMissingDeps) {
                    missingDeps.addAll(controller.getImmediateUnavailableDependencies());
                }

                final Set<ServiceName> previousMissing = previousMissingDepSet;

                // no longer missing deps...
                final Set<ServiceName> noLongerMissing = new TreeSet<ServiceName>();
                for (ServiceName name : previousMissing) {
                    if (! missingDeps.contains(name)) {
                        noLongerMissing.add(name);
                    }
                }

                // newly missing deps
                final Set<ServiceName> newlyMissing = new TreeSet<ServiceName>();
                newlyMissing.clear();
                for (ServiceName name : missingDeps) {
                    if (! previousMissing.contains(name)) {
                        newlyMissing.add(name);
                    }
                }

                previousMissingDepSet = missingDeps;

                // track failed services for the change report
                latestSettledFailedControllers.clear();
                latestSettledFailedControllers.putAll(failedControllers);

                final StringBuilder msg = new StringBuilder();
                msg.append("Service status report\n");
                boolean print = false;
                if (! newlyMissing.isEmpty()) {
                    print = true;
                    msg.append("   New missing/unsatisfied dependencies:\n");
                    for (ServiceName name : newlyMissing) {
                        ServiceController<?> controller = serviceRegistry.getService(name);
                        if (controller == null) {
                            msg.append("      ").append(name).append(" (missing)\n");
                        } else {
                            msg.append("      ").append(name).append(" (unavailable)\n");
                        }
                    }
                }
                if (! noLongerMissing.isEmpty()) {
                    print = true;
                    msg.append("   Newly corrected services:\n");
                    for (ServiceName name : noLongerMissing) {
                        ServiceController<?> controller = serviceRegistry.getService(name);
                        if (controller == null) {
                            msg.append("      ").append(name).append(" (no longer required)\n");
                        } else {
                            msg.append("      ").append(name).append(" (now available)\n");
                        }
                    }
                }
                if (! failedControllers.isEmpty()) {
                    print = true;
                    msg.append("  Services which failed to start:\n");
                    for (Map.Entry<ServiceController<?>, String> entry : failedControllers.entrySet()) {
                        msg.append("      ").append(entry.getKey().getName()).append(": ").append(entry.getValue()).append('\n');
                    }
                    failedControllers.clear();
                }
                if (print) {
                    log.info(msg);
                }
            }
        }
    }

    private void untick() {
        busyServiceCount.incrementAndGet();
    }

    synchronized ModelNode getServerStateChangeReport() {

        // Determine the newly failed controllers
        final Map<ServiceController<?>, String> newFailedControllers = new IdentityHashMap<ServiceController<?>, String>(latestSettledFailedControllers);
        newFailedControllers.keySet().removeAll(lastReportFailedControllers.keySet());
        // Back up current state for use in next report
        lastReportFailedControllers.clear();
        lastReportFailedControllers.putAll(latestSettledFailedControllers);
        // Determine the new missing dependencies
        final Set<ServiceName> newReportMissingDepSet = new TreeSet<ServiceName>(previousMissingDepSet);
        newReportMissingDepSet.removeAll(lastReportMissingDepSet);
        // Back up current state for use in next report
        lastReportMissingDepSet.clear();
        lastReportMissingDepSet.addAll(previousMissingDepSet);

        ModelNode report = null;
        if (!newFailedControllers.isEmpty() || !newReportMissingDepSet.isEmpty()) {
            report = new ModelNode();
            if (! newReportMissingDepSet.isEmpty()) {
                ModelNode missing = report.get("New missing/unsatisfied dependencies");
                for (ServiceName name : newReportMissingDepSet) {
                    ServiceController<?> controller = serviceRegistry.getService(name);
                    if (controller == null) {
                        missing.add(name + " (missing)");
                    } else {
                        missing.add(name + " (unavailable)\n");
                    }
                }
            }
            if (! newFailedControllers.isEmpty()) {
                ModelNode failed = report.get("Services which failed to start:");
                for (Map.Entry<ServiceController<?>, String> entry : newFailedControllers.entrySet()) {
                    failed.add(entry.getKey().getName().toString());
                }
            }
        }
        return report;
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
    }
}
