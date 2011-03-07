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

package org.jboss.as.server.mgmt;

import java.io.File;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ModelController;
import org.jboss.as.domain.http.server.ManagementHttpServer;
import org.jboss.as.server.services.net.NetworkInterfaceBinding;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * A service which launches the domain HTTP API and serverManagement.
 *
 * @author Jason T. Greene
 */
public class HttpManagementService implements Service<HttpManagementService>  {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("serverManagement", "controller", "management", "http");

    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<Integer> portValue = new InjectedValue<Integer>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<ExecutorService>();
    private final InjectedValue<String> tempDirValue = new InjectedValue<String>();
    private ManagementHttpServer serverManagement;

    /**
     * Starts the service.
     *
     * @param context The start context
     * @throws StartException If any errors occur
     */
    public synchronized void start(StartContext context) throws StartException {
        final ModelController modelController = modelControllerValue.getValue();
        final ExecutorService executorService = executorServiceValue.getValue();
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getValue();
        final Integer port = portValue.getValue();
        final InetSocketAddress bindAddress = new InetSocketAddress(interfaceBinding.getAddress(), port);

        try {
            serverManagement = ManagementHttpServer.create(bindAddress, 50, modelController, executorService);
            serverManagement.start();
        } catch (Exception e) {
            throw new StartException("Failed to start serverManagement socket", e);
        }
    }

    /**
     * Stops the service.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        if (serverManagement != null) {
            serverManagement.stop();
        }
    }

    /** {@inheritDoc} */
    public HttpManagementService getValue() throws IllegalStateException {
        return this;
    }

    /**
     * Get the interface binding injector.
     *
     * @return The injector
     */
    public Injector<NetworkInterfaceBinding> getInterfaceInjector() {
        return interfaceBindingValue;
    }

    /**
     * Get the executor service injector.
     *
     * @return The injector
     */
    public Injector<ExecutorService> getExecutorServiceInjector() {
        return executorServiceValue;
    }

    /**
     * Get the management port injector.
     *
     * @return The injector
     */
    public Injector<Integer> getPortInjector() {
        return portValue;
    }

    /**
     * Get the model controller injector to dispatch management requests to
     *
     * @return the injector
     */
    public Injector<ModelController> getModelControllerInjector() {
        return modelControllerValue;
    }

    /**
     * Get the temp dir injector.
     *
     * @return the tempDirValue
     */
    public InjectedValue<String> getTempDirInjector() {
        return tempDirValue;
    }

}
