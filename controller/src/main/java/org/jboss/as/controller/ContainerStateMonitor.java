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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.as.controller.logging.ControllerLogger;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartException;

import static org.jboss.as.controller.logging.ControllerLogger.ROOT_LOGGER;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ContainerStateMonitor extends AbstractServiceListener<Object> {

    private final ServiceRegistry serviceRegistry;
    private final StabilityMonitor monitor = new StabilityMonitor();
    final Set<ServiceController<?>> failed = new HashSet<ServiceController<?>>();
    final Set<ServiceController<?>> problems = new HashSet<ServiceController<?>>();

    private Set<ServiceName> previousMissingDepSet = new HashSet<ServiceName>();

    ContainerStateMonitor(final ServiceRegistry registry) {
        serviceRegistry = registry;
    }

    /**
     * Log a report of any problematic container state changes and reset container state change history
     * so another run of this method or of {@link #awaitContainerStateChangeReport(long, java.util.concurrent.TimeUnit)}
     * will produce a report not including any changes included in a report returned by this run.
     */
    void logContainerStateChangesAndReset() {
        ContainerStateChangeReport changeReport = createContainerStateChangeReport(true);

        if (changeReport != null) {
            final String msg = createChangeReportLogMessage(changeReport);
            ROOT_LOGGER.info(msg);
        }
    }

    @Override
    public void listenerAdded(final ServiceController<?> controller) {
        monitor.addController(controller);
        controller.removeListener(this);
    }

    /**
     * Await service container stability ignoring thread interruption.
     *
     * @param timeout maximum period to wait for service container stability
     * @param timeUnit unit in which {@code timeout} is expressed
     *
     * @throws java.util.concurrent.TimeoutException if service container stability is not reached before the specified timeout
     */
    void awaitStabilityUninterruptibly(long timeout, TimeUnit timeUnit) throws TimeoutException {
        boolean interrupted = false;
        try {
            long toWait = timeUnit.toMillis(timeout);
            long msTimeout = System.currentTimeMillis() + toWait;
            while (true) {
                if (interrupted) {
                    toWait = msTimeout - System.currentTimeMillis();
                }
                try {
                    if (toWait <= 0 || !monitor.awaitStability(toWait, TimeUnit.MILLISECONDS, failed, problems)) {
                        throw new TimeoutException();
                    }
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Await service container stability.
     *
     * @param timeout maximum period to wait for service container stability
     * @param timeUnit unit in which {@code timeout} is expressed
     *
     * @throws java.lang.InterruptedException if the thread is interrupted while awaiting service container stability
     * @throws java.util.concurrent.TimeoutException if service container stability is not reached before the specified timeout
     */
    void awaitStability(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        if (!monitor.awaitStability(timeout, timeUnit, failed, problems)) {
            throw new TimeoutException();
        }
    }

    /**
     * Await service container stability and then report on container state changes. Does not reset change history,
     * so another run of this method with no intervening call to {@link #logContainerStateChangesAndReset()}
     * will produce a report including any changes included in a report returned by the first run.
     *
     * @param timeout maximum period to wait for service container stability
     * @param timeUnit unit in which {@code timeout} is expressed
     *
     * @return a change report, or {@code null} if there is nothing to report
     *
     * @throws java.lang.InterruptedException if the thread is interrupted while awaiting service container stability
     * @throws java.util.concurrent.TimeoutException if service container stability is not reached before the specified timeout
     */
    ContainerStateChangeReport awaitContainerStateChangeReport(long timeout, TimeUnit timeUnit) throws InterruptedException, TimeoutException {
        if (monitor.awaitStability(timeout, timeUnit, failed, problems)) {
            return createContainerStateChangeReport(false);
        }
        throw new TimeoutException();
    }

    /**
     * Creates a data structure reporting recent favorable and unfavorable changes in the state of installed services.
     *
     * @param resetHistory {@code true} if history tracking state used for detecting what has changed on the next
     *                                 invocation of this method should be reset (meaning the next run will detect
     *                                 more changes); {@code false} if the current history should be retained
     *                                 (meaning the next run will act as if this run never happened)
     *
     * @return the report, or {@code null} if there is nothing noteworthy to report; i.e. no newly failed or missing
     *         services and no newly corrected services
     */
    private synchronized ContainerStateChangeReport createContainerStateChangeReport(boolean resetHistory) {

        final Map<ServiceName, Set<ServiceName>> missingDeps = new HashMap<ServiceName, Set<ServiceName>>();
        for (ServiceController<?> controller : problems) {
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
                noLongerMissingServices.put(name, controller != null);
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

        final Set<ServiceController<?>> currentFailedControllers = new HashSet<ServiceController<?>>(failed);

        if (resetHistory)  {
            previousMissingDepSet = new HashSet<ServiceName>(missingDeps.keySet());
            failed.clear();
            problems.clear();
        }

        boolean needReport = !missingServices.isEmpty() || !currentFailedControllers.isEmpty() || !noLongerMissingServices.isEmpty();
        return needReport ? new ContainerStateChangeReport(missingServices, currentFailedControllers, noLongerMissingServices) : null;
    }

    private synchronized String createChangeReportLogMessage(ContainerStateChangeReport changeReport) {

        final StringBuilder msg = new StringBuilder();
        msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportHeader());
        if (!changeReport.getMissingServices().isEmpty()) {
            msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportDependencies());
            for (Map.Entry<ServiceName, MissingDependencyInfo> entry : changeReport.getMissingServices().entrySet()) {
                if (!entry.getValue().isUnavailable()) {
                    msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportMissing(entry.getKey(), createDependentsString(entry.getValue().getDependents())));
                } else {
                    msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportUnavailable(entry.getKey(), createDependentsString(entry.getValue().getDependents())));
                }
            }
        }
        if (!changeReport.getNoLongerMissingServices().isEmpty()) {
            msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportCorrected());
            for (Map.Entry<ServiceName, Boolean> entry : changeReport.getNoLongerMissingServices().entrySet()) {
                if (entry.getValue()) {
                    msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportAvailable(entry.getKey()));
                } else {
                    msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportNoLongerRequired(entry.getKey()));
                }
            }
        }
        if (!changeReport.getFailedControllers().isEmpty()) {
            msg.append(ControllerLogger.ROOT_LOGGER.serviceStatusReportFailed());
            for (ServiceController<?> controller : changeReport.getFailedControllers()) {
                msg.append("      ").append(controller.getName());
                //noinspection ThrowableResultOfMethodCallIgnored
                final StartException startException = controller.getStartException();
                if (startException != null) {
                    msg.append(": ").append(startException.toString());
                }
                msg.append('\n');
            }
        }
        return msg.toString();
    }

    public static class ContainerStateChangeReport {

        private final Map<ServiceName, MissingDependencyInfo> missingServices;
        private final Set<ServiceController<?>> failedControllers;
        private final Map<ServiceName, Boolean> noLongerMissingServices;

        private ContainerStateChangeReport(final Map<ServiceName, MissingDependencyInfo> missingServices,
                                           final Set<ServiceController<?>> failedControllers,
                                           final Map<ServiceName, Boolean> noLongerMissingServices) {
            this.missingServices = missingServices;
            this.failedControllers = failedControllers;
            this.noLongerMissingServices = noLongerMissingServices;
        }

        public final Set<ServiceController<?>> getFailedControllers() {
            return failedControllers;
        }

        public Map<ServiceName, MissingDependencyInfo> getMissingServices() {
            return missingServices;
        }

        /**
         * Gets services that are no longer considered to be missing.
         * @return a map of the service name of the no-longer-missing service to a boolean indicating
         *          whether or not the service now exists ({@code true} if it does.) If {@code false}
         *          the service is no longer "missing" because it is no longer depended upon
         */
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
            ret.append(ControllerLogger.ROOT_LOGGER.andNMore(serviceNames.size() - 3));
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
}
