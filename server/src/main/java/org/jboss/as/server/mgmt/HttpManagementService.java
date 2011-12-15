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

import java.net.BindException;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

import org.jboss.as.controller.ModelController;
import org.jboss.as.controller.RunningMode;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.domain.http.server.ConsoleMode;
import org.jboss.as.domain.http.server.ManagementHttpServer;
import org.jboss.as.domain.management.security.SecurityRealmService;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.ManagedBindingRegistry;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.mgmt.domain.HttpManagement;
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
public class HttpManagementService implements Service<HttpManagement> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("serverManagement", "controller", "management", "http");

    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<SocketBinding> injectedSocketBindingValue = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> injectedSecureSocketBindingValue = new InjectedValue<SocketBinding>();
    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<SocketBindingManager> injectedSocketBindingManager = new InjectedValue<SocketBindingManager>();
    private final InjectedValue<Integer> portValue = new InjectedValue<Integer>();
    private final InjectedValue<Integer> securePortValue = new InjectedValue<Integer>();
    private final InjectedValue<ExecutorService> executorServiceValue = new InjectedValue<ExecutorService>();
    private final InjectedValue<SecurityRealmService> securityRealmServiceValue = new InjectedValue<SecurityRealmService>();
    private final ConsoleMode consoleMode;
    private ManagementHttpServer serverManagement;
    private SocketBindingManager socketBindingManager;
    private boolean useUnmanagedBindings = false;
    private ManagedBinding basicManagedBinding;
    private ManagedBinding secureManagedBinding;

    private HttpManagement httpManagement = new HttpManagement() {
        public InetSocketAddress getHttpSocketAddress(){
            return basicManagedBinding == null ? null : basicManagedBinding.getBindAddress();
        }

        public InetSocketAddress getHttpsSocketAddress() {
            return secureManagedBinding == null ? null : secureManagedBinding.getBindAddress();
        }

        @Override
        public int getHttpPort() {
            return basicManagedBinding == null ? -1 : basicManagedBinding.getBindAddress().getPort();
        }

        @Override
        public NetworkInterfaceBinding getHttpNetworkInterfaceBinding() {
            NetworkInterfaceBinding binding = interfaceBindingValue.getOptionalValue();
            if (binding == null) {
                SocketBinding socketBinding = injectedSocketBindingValue.getOptionalValue();
                if (socketBinding != null) {
                    binding = socketBinding.getNetworkInterfaceBinding();
                }
            }
            return binding;
        }

        @Override
        public int getHttpsPort() {
            return secureManagedBinding == null ? -1 : secureManagedBinding.getBindAddress().getPort();
        }

        @Override
        public NetworkInterfaceBinding getHttpsNetworkInterfaceBinding() {
            NetworkInterfaceBinding binding = interfaceBindingValue.getOptionalValue();
            if (binding == null) {
                SocketBinding socketBinding = injectedSecureSocketBindingValue.getOptionalValue();
                if (socketBinding != null) {
                    binding = socketBinding.getNetworkInterfaceBinding();
                }
            }
            return binding;
        }
    };

    public HttpManagementService(ConsoleMode consoleMode) {
        this.consoleMode = consoleMode;
    }

    public HttpManagementService createForStandalone(RunningMode runningMode) {
        return new HttpManagementService(runningMode == RunningMode.ADMIN_ONLY ? ConsoleMode.ADMIN_ONLY : ConsoleMode.CONSOLE);
    }

    public HttpManagementService createForHost(RunningMode runningMode) {
        return new HttpManagementService(runningMode == RunningMode.ADMIN_ONLY ? ConsoleMode.ADMIN_ONLY : ConsoleMode.CONSOLE);
    }

    /**
     * Starts the service.
     *
     * @param context The start context
     * @throws StartException If any errors occur
     */
    public synchronized void start(StartContext context) throws StartException {
        final ModelController modelController = modelControllerValue.getValue();
        final ExecutorService executorService = executorServiceValue.getValue();
        final ModelControllerClient modelControllerClient = modelController.createClient(executorService);
        socketBindingManager = injectedSocketBindingManager.getOptionalValue();

        final SecurityRealmService securityRealmService = securityRealmServiceValue.getOptionalValue();

        InetSocketAddress bindAddress = null;
        InetSocketAddress secureBindAddress = null;

        SocketBinding basicBinding = injectedSocketBindingValue.getOptionalValue();
        SocketBinding secureBinding = injectedSecureSocketBindingValue.getOptionalValue();
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getOptionalValue();
        if (interfaceBinding != null) {
            useUnmanagedBindings = true;
            final int port = portValue.getOptionalValue();
            if (port > 0) {
                bindAddress = new InetSocketAddress(interfaceBinding.getAddress(), port);
            }
            final int securePort = securePortValue.getOptionalValue();
            if (securePort > 0) {
                secureBindAddress = new InetSocketAddress(interfaceBinding.getAddress(), securePort);
            }
        } else {
            if (basicBinding != null) {
                bindAddress = basicBinding.getSocketAddress();
            }
            if (secureBinding != null) {
                secureBindAddress = secureBinding.getSocketAddress();
            }
        }

        try {
            serverManagement = ManagementHttpServer.create(bindAddress, secureBindAddress, 50, modelControllerClient, executorService, securityRealmService, consoleMode);
            serverManagement.start();

            // Register the now-created sockets with the SBM
            if (socketBindingManager != null) {
                if (useUnmanagedBindings) {
                    SocketBindingManager.UnnamedBindingRegistry registry = socketBindingManager.getUnnamedRegistry();
                    if (bindAddress != null) {
                        basicManagedBinding = ManagedBinding.Factory.createSimpleManagedBinding("management-http", bindAddress, null);
                        registry.registerBinding(basicManagedBinding);
                    }
                    if (secureBindAddress != null) {
                        secureManagedBinding = ManagedBinding.Factory.createSimpleManagedBinding("management-https", secureBindAddress, null);
                        registry.registerBinding(secureManagedBinding);
                    }
                } else {
                    SocketBindingManager.NamedManagedBindingRegistry registry = socketBindingManager.getNamedRegistry();
                    if (basicBinding != null) {
                        basicManagedBinding = ManagedBinding.Factory.createSimpleManagedBinding(basicBinding);
                        registry.registerBinding(basicManagedBinding);
                    }
                    if (secureBinding != null) {
                        secureManagedBinding = ManagedBinding.Factory.createSimpleManagedBinding(secureBinding);
                        registry.registerBinding(secureManagedBinding);
                    }
                }
            }
        } catch (BindException e) {
            final StringBuilder sb = new StringBuilder().append(e.getMessage());
            if (bindAddress != null)
                sb.append(" ").append(bindAddress);
            if (secureBindAddress != null)
                sb.append(" ").append(secureBindAddress);
            throw new StartException(sb.toString(), e);
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
            try {
                serverManagement.stop();
            } finally {
                serverManagement = null;

                // Unregister sockets from the SBM
                if (socketBindingManager != null) {
                    ManagedBindingRegistry registry = useUnmanagedBindings ? socketBindingManager.getUnnamedRegistry() : socketBindingManager.getNamedRegistry();
                    if (basicManagedBinding != null) {
                        registry.unregisterBinding(basicManagedBinding);
                        basicManagedBinding = null;
                    }
                    if (secureManagedBinding != null) {
                        registry.unregisterBinding(secureManagedBinding);
                        secureManagedBinding = null;
                    }
                    socketBindingManager = null;
                    useUnmanagedBindings = false;
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public HttpManagement getValue() throws IllegalStateException {
        return httpManagement;
    }

    /**
     * Get the interface binding injector.
     *
     * @return The injector
     */
    public Injector<NetworkInterfaceBinding> getInterfaceInjector() {
        return interfaceBindingValue;
    }

    public Injector<SocketBindingManager> getSocketBindingManagerInjector() {
        return injectedSocketBindingManager;
    }

    public Injector<SocketBinding> getSocketBindingInjector() {
        return injectedSocketBindingValue;
    }

    public Injector<SocketBinding> getSecureSocketBindingInjector() {
        return injectedSecureSocketBindingValue;
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
     * Get the management secure port injector.
     *
     * @return The injector
     */
    public Injector<Integer> getSecurePortInjector() {
        return securePortValue;
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
     * Get the security realm injector.
     *
     * @return the securityRealmServiceValue
     */
    public InjectedValue<SecurityRealmService> getSecurityRealmInjector() {
        return securityRealmServiceValue;
    }

}
