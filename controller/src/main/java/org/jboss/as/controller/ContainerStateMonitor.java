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
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

import static org.jboss.as.controller.ControllerLogger.ROOT_LOGGER;
import static org.jboss.as.controller.ControllerMessages.MESSAGES;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ContainerStateMonitor extends AbstractServiceListener<Object> {

    private final ServiceRegistry serviceRegistry;
    private final ServiceController<?> controllerController;
    private final AtomicInteger busyServiceCount = new AtomicInteger();

    // protected by "this"
    /** Failed controllers pending tick reaching zero */
    private final Map<ServiceController<?>, String> failedControllers = new IdentityHashMap<ServiceController<?>, String>();
    /** Services with missing deps */
    private final Set<ServiceController<?>> servicesWithMissingDeps = identitySet();
    /** Services with missing deps as of the last time tick reached zero */
    private Set<ServiceName> previousMissingDepSet = new HashSet<ServiceName>();
    /** State report generated the last time tick reached zero or awaitContainerStateChangeReport was called  */
    private ContainerStateChangeReport changeReport;

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

    void await(int count) throws InterruptedException {
        synchronized (this) {
            while (busyServiceCount.get() > count) {
                wait();
            }
        }
    }

    ContainerStateChangeReport awaitContainerStateChangeReport(int count) throws InterruptedException {
        synchronized (this) {
            while (busyServiceCount.get() > count) {
                wait();
            }
            changeReport = createContainerStateChangeReport();
            return changeReport;
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
                if (changeReport == null) {
                    changeReport = createContainerStateChangeReport();
                }
                // else someone called awaitContainerStateChangeReport -- use the one created by that

                if (changeReport != null) {
                    final String msg = createChangeReportLogMessage(changeReport);
                    changeReport = null;
                    ROOT_LOGGER.info(msg);
                }
            }
        }
    }

    private void untick() {
        busyServiceCount.incrementAndGet();
    }

    private synchronized ContainerStateChangeReport createContainerStateChangeReport() {

        final Map<ServiceName, Set<ServiceName>> missingDeps = new HashMap<ServiceName, Set<ServiceName>>();
        for (ServiceController<?> controller : servicesWithMissingDeps) {
            for (ServiceName missing : controller.getImmediateUnavailableDependencies()) {
                Set<ServiceName> dependents = missingDeps.get(missing);
                if (dependents == null) {
                    dependents = new HashSet<ServiceName>();
                    missingDeps.put(missing, dependents);
                }
                dependents.add(controller.getName());
            }
        }

        final Set<ServiceName> previousMissing = previousMissingDepSet;

        // no longer missing deps...
        final Map<ServiceName, Boolean> noLongerMissingServices = new TreeMap<ServiceName, Boolean>();
        for (ServiceName name : previousMissing) {
            if (! missingDeps.containsKey(name)) {
                ServiceController<?> controller = serviceRegistry.getService(name);
                noLongerMissingServices.put(name, controller == null);
            }
        }

        // newly missing deps
        final Map<ServiceName, MissingDependencyInfo> missingServices = new TreeMap<ServiceName, MissingDependencyInfo>();
        for (Map.Entry<ServiceName, Set<ServiceName>> entry : missingDeps.entrySet()) {
            final ServiceName name = entry.getKey();
            if (! previousMissing.contains(name)) {
                ServiceController<?> controller = serviceRegistry.getService(name);
                boolean unavailable = controller != null;
                missingServices.put(name, new MissingDependencyInfo(name, unavailable, entry.getValue()));
            }
        }

        final Map<ServiceController<?>, String> currentFailedControllers = new HashMap<ServiceController<?>, String>(failedControllers);

        previousMissingDepSet = new HashSet<ServiceName>(missingDeps.keySet());

        failedControllers.clear();

        boolean needReport = !missingServices.isEmpty() || !currentFailedControllers.isEmpty() || !noLongerMissingServices.isEmpty();
        return needReport ? new ContainerStateChangeReport(missingServices, currentFailedControllers, noLongerMissingServices) : null;
    }

    private synchronized String createChangeReportLogMessage(ContainerStateChangeReport changeReport) {

        final StringBuilder msg = new StringBuilder();
        msg.append(MESSAGES.serviceStatusReportHeader());
        if (!changeReport.getMissingServices().isEmpty()) {
            msg.append(MESSAGES.serviceStatusReportDependencies());
            for (Map.Entry<ServiceName, MissingDependencyInfo> entry : changeReport.getMissingServices().entrySet()) {
                if (!entry.getValue().isUnavailable()) {
                    msg.append(MESSAGES.serviceStatusReportMissing(entry.getKey(), createDependentsString(entry.getValue().getDependents())));
                } else {
                    msg.append(MESSAGES.serviceStatusReportUnavailable(entry.getKey(), createDependentsString(entry.getValue().getDependents())));
                }
            }
        }
        if (!changeReport.getNoLongerMissingServices().isEmpty()) {
            msg.append(MESSAGES.serviceStatusReportCorrected());
            for (Map.Entry<ServiceName, Boolean> entry : changeReport.getNoLongerMissingServices().entrySet()) {
                if (!entry.getValue()) {
                    msg.append(MESSAGES.serviceStatusReportNoLongerRequired(entry.getKey()));
                } else {
                    msg.append(MESSAGES.serviceStatusReportAvailable(entry.getKey()));
                }
            }
        }
        if (!changeReport.getFailedControllers().isEmpty()) {
            msg.append(MESSAGES.serviceStatusReportFailed());
            for (Map.Entry<ServiceController<?>, String> entry : changeReport.getFailedControllers().entrySet()) {
                msg.append("      ").append(entry.getKey().getName()).append(": ").append(entry.getValue()).append('\n');
            }

        }
        return msg.toString();
    }

    public static class ContainerStateChangeReport {

        private final Map<ServiceName, MissingDependencyInfo> missingServices;
        private final Map<ServiceController<?>, String> failedControllers;
        private final Map<ServiceName, Boolean> noLongerMissingServices;

        private ContainerStateChangeReport(final Map<ServiceName, MissingDependencyInfo> missingServices,
                                           final Map<ServiceController<?>, String> failedControllers,
                                           final Map<ServiceName, Boolean> noLongerMissingServices) {
            this.missingServices = missingServices;
            this.failedControllers = failedControllers;
            this.noLongerMissingServices = noLongerMissingServices;
        }

        public final Map<ServiceController<?>, String> getFailedControllers() {
            return failedControllers;
        }

        public Map<ServiceName, MissingDependencyInfo> getMissingServices() {
            return missingServices;
        }

        public Map<ServiceName, Boolean> getNoLongerMissingServices() {
            return noLongerMissingServices;
        }
    }

    private static String createDependentsString(final Set<ServiceName> serviceNames) {
        if(serviceNames.size() <= 4) {
            return serviceNames.toString();
        } else {
            final StringBuilder ret = new StringBuilder("[");
            int count = 0;
            Iterator<ServiceName> it = serviceNames.iterator();
            while (count < 4) {
                final ServiceName val = it.next();
                ret.append(val);
                ret.append(", ");
                ++count;
            }
            ret.append(MESSAGES.andNMore(serviceNames.size() - 3));
            ret.append(" ]");
            return ret.toString();
        }
    }

    public static class MissingDependencyInfo {
        private final ServiceName serviceName;
        private final boolean unavailable;
        private final Set<ServiceName> dependents;

        public MissingDependencyInfo(ServiceName serviceName, boolean unavailable, final Set<ServiceName> dependents) {
            this.serviceName = serviceName;
            this.unavailable = unavailable;
            this.dependents = dependents;
        }

        public ServiceName getServiceName() {
            return serviceName;
        }

        public boolean isUnavailable() {
            return unavailable;
        }

        public Set<ServiceName> getDependents() {
            return Collections.unmodifiableSet(dependents);
        }
    }

    private static <T> Set<T> identitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<T, Boolean>());
    }
}
