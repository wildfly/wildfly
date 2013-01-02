/*
* JBoss, Home of Professional Open Source.
* Copyright 2012, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.host.controller;

import static org.jboss.as.host.controller.HostControllerLogger.ROOT_LOGGER;

import java.util.List;
import java.util.concurrent.ExecutorService;

import org.jboss.as.host.controller.discovery.DiscoveryOption;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.server.services.net.NetworkInterfaceService;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service responsible for allowing a domain controller to be discovered by
 * slave host controllers.
 *
 * @author Farah Juma
 */
class DiscoveryService implements Service<Void> {

    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("host", "controller", "discovery");

    private final InjectedValue<NetworkInterfaceBinding> interfaceBinding = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<ExecutorService> executorService = new InjectedValue<ExecutorService>();
    private final List<DiscoveryOption> discoveryOptions;
    private final int port;
    private final boolean isMasterDomainController;

    /**
     * Create the DiscoveryService instance.
     *
     * @param discoveryOptions the list of discovery options
     * @param port the port number of the domain controller
     * @param isMasterDomainController whether or not the local host controller is the master
     */
    private DiscoveryService(List<DiscoveryOption> discoveryOptions, int port, boolean isMasterDomainController) {
        this.discoveryOptions = discoveryOptions;
        this.port = port;
        this.isMasterDomainController = isMasterDomainController;
    }

    static void install(final ServiceTarget serviceTarget, final List<DiscoveryOption> discoveryOptions,
                        final String interfaceBinding, final int port, final boolean isMasterDomainController) {
        final DiscoveryService discovery = new DiscoveryService(discoveryOptions, port, isMasterDomainController);
        serviceTarget.addService(DiscoveryService.SERVICE_NAME, discovery)
            .addDependency(HostControllerService.HC_EXECUTOR_SERVICE_NAME, ExecutorService.class, discovery.executorService)
            .addDependency(NetworkInterfaceService.JBOSS_NETWORK_INTERFACE.append(interfaceBinding), NetworkInterfaceBinding.class, discovery.interfaceBinding)
            .install();
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(final StartContext context) throws StartException {
        context.asynchronous();
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    if (isMasterDomainController && (discoveryOptions != null)) {
                        // Allow slave host controllers to discover this domain controller using any
                        // of the provided discovery options.
                        String host = interfaceBinding.getValue().getAddress().getHostAddress();
                        for (DiscoveryOption discoveryOption : discoveryOptions) {
                            discoveryOption.allowDiscovery(host, port);
                        }
                    }
                    context.complete();
                } catch (Exception e) {
                    context.failed(new StartException(e));
                }
            }
        };
        executorService.getValue().execute(r);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(final StopContext context) {
        context.asynchronous();
        Runnable r = new Runnable() {

            @Override
            public void run() {
                try {
                    if (isMasterDomainController && (discoveryOptions != null)) {
                        for (DiscoveryOption discoveryOption : discoveryOptions) {
                            discoveryOption.cleanUp();
                        }
                    }
                } finally {
                    context.complete();
                }
            }
        };
        executorService.getValue().execute(r);
    }

    /** {@inheritDoc} */
    @Override
    public synchronized Void getValue() throws IllegalStateException, IllegalArgumentException {
       return null;
    }
}
