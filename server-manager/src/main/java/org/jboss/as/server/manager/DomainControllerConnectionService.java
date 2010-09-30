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

package org.jboss.as.server.manager;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import org.jboss.as.services.net.NetworkInterfaceBinding;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Domain controller client service.  This will connect to a domain controller to manage inter-process communication.
 * Once the connection is established this will register itself with the domain controller and start listening for
 * commands from the domain controller.
 *
 * @author John E. Bailey
 */
public class DomainControllerConnectionService implements Service<Void> {
    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "client");
    private static final Logger log = Logger.getLogger("org.jboss.as.server.manager");

    private final InjectedValue<InetAddress> domainControllerAddress = new InjectedValue<InetAddress>();
    private final InjectedValue<Integer> domainControllerPort = new InjectedValue<Integer>();

    private final InjectedValue<NetworkInterfaceBinding> localManagementInterface = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<Integer> localManagementPort = new InjectedValue<Integer>();

    private final InjectedValue<ScheduledExecutorService> executorService = new InjectedValue<ScheduledExecutorService>();

    private final ServerManager serverManager;
    private final FileRepository localRepository;

    private final int connectionRetryLimit;
    private final long connectionRetryInterval;
    private final long connectTimeout;

    public DomainControllerConnectionService(final ServerManager serverManager, final FileRepository localRepository, final int connectionRetryLimit, final long connectionRetryInterval, final long connectTimeout) {
        this.serverManager = serverManager;
        this.localRepository = localRepository;
        this.connectionRetryLimit = connectionRetryLimit;
        this.connectionRetryInterval = connectionRetryInterval;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Start the service.  Setup a remote domain controller connection and hand it to the server manager.
     *
     * @param context The start context
     * @throws StartException
     */
    public synchronized void start(final StartContext context) throws StartException {
        InetAddress dcAddress = domainControllerAddress.getValue();
        if (dcAddress.isAnyLocalAddress() || dcAddress.isSiteLocalAddress()) {
            try {
                dcAddress = InetAddress.getLocalHost();
            } catch (UnknownHostException e) {
                throw new StartException("Failed to get domain controller address", e);
            }
        }
        final NetworkInterfaceBinding managementInterface = localManagementInterface.getValue();
        serverManager.setDomainControllerConnection(new RemoteDomainControllerConnection(serverManager.getName(), dcAddress, domainControllerPort.getValue(), managementInterface.getAddress(), localManagementPort.getValue(), localRepository, connectionRetryLimit, connectionRetryInterval, connectTimeout, executorService.getValue()));
    }

    /**
     * Stop the service.  Remove the domain controller connection from the server manager.
     *
     * @param context The stop context.
     */
    public synchronized void stop(final StopContext context) {

    }

    /**
     * No value for this service.
     *
     * @return {@code null}
     */
    public Void getValue() throws IllegalStateException {
        return null;
    }

    public Injector<InetAddress> getDomainControllerAddressInjector() {
        return domainControllerAddress;
    }

    public Injector<Integer> getDomainControllerPortInjector() {
        return domainControllerPort;
    }

    public Injector<NetworkInterfaceBinding> getLocalManagementInterfaceInjector() {
        return localManagementInterface;
    }

    public Injector<Integer> getLocalManagementPortInjector() {
        return localManagementPort;
    }

    public Injector<ScheduledExecutorService> getExecutorServiceInjector() {
        return executorService;
    }
}
