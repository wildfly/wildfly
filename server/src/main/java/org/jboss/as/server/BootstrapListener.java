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

import org.jboss.as.network.NetworkUtils;
import org.jboss.as.server.mgmt.HttpManagementService;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StabilityStatistics;

/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class BootstrapListener extends AbstractServiceListener<Object> {

    private final StabilityMonitor monitor = new StabilityMonitor();
    private final ServiceContainer serviceContainer;
    private final ServiceTarget serviceTarget;
    private final long startTime;
    private final String prettyVersion;
    private final FutureServiceContainer futureContainer;

    public BootstrapListener(final ServiceContainer serviceContainer, final long startTime, final ServiceTarget serviceTarget, final FutureServiceContainer futureContainer, final String prettyVersion) {
        this.serviceContainer = serviceContainer;
        this.startTime = startTime;
        this.serviceTarget = serviceTarget;
        this.prettyVersion = prettyVersion;
        this.futureContainer = futureContainer;
    }

    @Override
    public void listenerAdded(final ServiceController<?> controller) {
        monitor.addController(controller);
        controller.removeListener(this);
    }

    public void printBootStatistics() {
        final StabilityStatistics statistics = new StabilityStatistics();
        try {
            monitor.awaitStability(statistics);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        } finally {
            serviceTarget.removeListener(BootstrapListener.this);
            final long bootstrapTime = System.currentTimeMillis() - startTime;
            done(bootstrapTime, statistics);
            monitor.clear();
        }
    }

    protected void done(final long bootstrapTime, final StabilityStatistics statistics) {
        futureContainer.done(serviceContainer);
        if (statistics.getRemovedCount() > 0) {
            // Don't print boot statistics if there are removed services.
            // Most likely server received shutdown signal during the boot process.
            return;
        }

        logAdminConsole();

        final int active = statistics.getActiveCount();
        final int failed = statistics.getFailedCount();
        final int lazy = statistics.getLazyCount();
        final int never = statistics.getNeverCount();
        final int onDemand = statistics.getOnDemandCount();
        final int passive = statistics.getPassiveCount();
        final int problem = statistics.getProblemsCount();
        final int started = statistics.getStartedCount();
        if (failed == 0 && problem == 0) {
            ServerLogger.AS_ROOT_LOGGER.startedClean(prettyVersion, bootstrapTime, started, active + passive + onDemand + never + lazy, onDemand + passive + lazy);
        } else {
            ServerLogger.AS_ROOT_LOGGER.startedWitErrors(prettyVersion, bootstrapTime, started, active + passive + onDemand + never + lazy, failed + problem, onDemand + passive + lazy);
        }
    }

    private void logAdminConsole() {
        ServiceController<?> controller = serviceContainer.getService(HttpManagementService.SERVICE_NAME);
        if (controller != null) {
            HttpManagement mgmt = (HttpManagement)controller.getValue();

            boolean hasHttp = mgmt.getHttpNetworkInterfaceBinding() != null && mgmt.getHttpPort() > 0;
            boolean hasHttps = mgmt.getHttpsNetworkInterfaceBinding() != null && mgmt.getHttpsPort() > 0;
            if (hasHttp && hasHttps) {
                ServerLogger.AS_ROOT_LOGGER.logHttpAndHttpsManagement(NetworkUtils.formatAddress(mgmt.getHttpNetworkInterfaceBinding().getAddress()), mgmt.getHttpPort(), NetworkUtils.formatAddress(mgmt.getHttpsNetworkInterfaceBinding().getAddress()), mgmt.getHttpsPort());
                if (mgmt.hasConsole()) {
                    ServerLogger.AS_ROOT_LOGGER.logHttpAndHttpsConsole(NetworkUtils.formatAddress(mgmt.getHttpNetworkInterfaceBinding().getAddress()), mgmt.getHttpPort(), NetworkUtils.formatAddress(mgmt.getHttpsNetworkInterfaceBinding().getAddress()), mgmt.getHttpsPort());
                } else {
                    ServerLogger.AS_ROOT_LOGGER.logNoConsole();
                }
            } else if (hasHttp) {
                ServerLogger.AS_ROOT_LOGGER.logHttpManagement(NetworkUtils.formatAddress(mgmt.getHttpNetworkInterfaceBinding().getAddress()), mgmt.getHttpPort());
                if (mgmt.hasConsole()) {
                    ServerLogger.AS_ROOT_LOGGER.logHttpConsole(NetworkUtils.formatAddress(mgmt.getHttpNetworkInterfaceBinding().getAddress()), mgmt.getHttpPort());
                } else {
                    ServerLogger.AS_ROOT_LOGGER.logNoConsole();
                }
            } else if (hasHttps) {
                ServerLogger.AS_ROOT_LOGGER.logHttpsManagement(NetworkUtils.formatAddress(mgmt.getHttpsNetworkInterfaceBinding().getAddress()), mgmt.getHttpsPort());
                if (mgmt.hasConsole()) {
                    ServerLogger.AS_ROOT_LOGGER.logHttpsConsole(NetworkUtils.formatAddress(mgmt.getHttpsNetworkInterfaceBinding().getAddress()), mgmt.getHttpsPort());
                } else {
                    ServerLogger.AS_ROOT_LOGGER.logNoConsole();
                }
            } else {
                ServerLogger.AS_ROOT_LOGGER.logNoHttpManagement();
                ServerLogger.AS_ROOT_LOGGER.logNoConsole();
            }
        }
    }
}
