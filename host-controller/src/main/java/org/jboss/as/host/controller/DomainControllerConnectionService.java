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

package org.jboss.as.host.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import org.jboss.as.domain.controller.FileRepository;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
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
public class DomainControllerConnectionService implements Service<DomainControllerConnection> {
    static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("domain", "controller", "client");

    private final InjectedValue<InetAddress> domainControllerAddress = new InjectedValue<InetAddress>();
    private final InjectedValue<Integer> domainControllerPort = new InjectedValue<Integer>();

    private final InjectedValue<NetworkInterfaceBinding> localManagementInterface = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<Integer> localManagementPort = new InjectedValue<Integer>();

    private final InjectedValue<ScheduledExecutorService> executorService = new InjectedValue<ScheduledExecutorService>();

    private final InjectedValue<ThreadFactory> threadFactoryValue = new InjectedValue<ThreadFactory>();

    private final HostController hostController;
    private final FileRepository localRepository;

    private final long connectTimeout;

    private DomainControllerConnection domainControllerConnection;

    public DomainControllerConnectionService(final HostController hostController, final FileRepository localRepository, final long connectTimeout) {
        this.hostController = hostController;
        this.localRepository = localRepository;
        this.connectTimeout = connectTimeout;
    }

    /**
     * Start the service.  Setup a remote domain controller connection and hand it to the host controller.
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
        domainControllerConnection = new RemoteDomainControllerConnection(hostController.getName(), dcAddress, domainControllerPort.getValue(), managementInterface.getAddress(), localManagementPort.getValue(), localRepository, connectTimeout, executorService.getValue(), threadFactoryValue.getValue());
    }

    /**
     * Stop the service.  Remove the domain controller connection from the host controller.
     *
     * @param context The stop context.
     */
    public synchronized void stop(final StopContext context) {
        domainControllerConnection = null;
    }

    /**
     * No value for this service.
     *
     * @return a remote domain controller connection
     */
    public DomainControllerConnection getValue() throws IllegalStateException {
        return domainControllerConnection;
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

    public Injector<ThreadFactory> getThreadFactoryInjector() {
        return threadFactoryValue;
    }
}
