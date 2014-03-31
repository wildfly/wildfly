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
package org.jboss.as.server.mgmt;

import io.undertow.server.ListenerRegistry;
import io.undertow.server.handlers.ChannelUpgradeHandler;

import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

import org.jboss.as.controller.ControlledProcessStateService;
import org.jboss.as.controller.ModelController;
import org.jboss.as.domain.http.server.ConsoleMode;
import org.jboss.as.domain.http.server.ManagementHttpServer;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.ManagedBindingRegistry;
import org.jboss.as.network.NetworkInterfaceBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.server.ServerMessages;
import org.jboss.as.server.mgmt.domain.HttpManagement;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class UndertowHttpManagementService implements Service<HttpManagement> {
    public static final ServiceName SERVICE_NAME = ServiceName.JBOSS.append("serverManagement", "controller", "management", "http");

    public static final String SERVER_NAME = "wildfly-managment";
    public static final String HTTP_MANAGEMENT = "http-management";
    public static final String HTTPS_MANAGEMENT = "https-management";

    public static final ServiceName HTTP_UPGRADE_SERVICE_NAME = ServiceName.JBOSS.append("http-upgrade-registry", HTTP_MANAGEMENT);
    public static final ServiceName HTTPS_UPGRADE_SERVICE_NAME = ServiceName.JBOSS.append("http-upgrade-registry", HTTPS_MANAGEMENT);
    public static final String JBOSS_REMOTING = "jboss-remoting";
    public static final String MANAGEMENT_ENDPOINT = "management-endpoint";

    private final InjectedValue<ListenerRegistry> listenerRegistry = new InjectedValue<>();
    private final InjectedValue<ModelController> modelControllerValue = new InjectedValue<ModelController>();
    private final InjectedValue<SocketBinding> injectedSocketBindingValue = new InjectedValue<SocketBinding>();
    private final InjectedValue<SocketBinding> injectedSecureSocketBindingValue = new InjectedValue<SocketBinding>();
    private final InjectedValue<NetworkInterfaceBinding> interfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<NetworkInterfaceBinding> secureInterfaceBindingValue = new InjectedValue<NetworkInterfaceBinding>();
    private final InjectedValue<SocketBindingManager> injectedSocketBindingManager = new InjectedValue<SocketBindingManager>();
    private final InjectedValue<Integer> portValue = new InjectedValue<Integer>();
    private final InjectedValue<Integer> securePortValue = new InjectedValue<Integer>();
    private final InjectedValue<SecurityRealm> securityRealmServiceValue = new InjectedValue<SecurityRealm>();
    private final InjectedValue<ControlledProcessStateService> controlledProcessStateServiceValue = new InjectedValue<ControlledProcessStateService>();
    private final ConsoleMode consoleMode;
    private final String consoleSlot;
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
            if (basicManagedBinding != null) {
                return basicManagedBinding.getBindAddress().getPort();
            }
            Integer port = portValue.getOptionalValue();
            if (port != null) {
                return port;
            }
            return -1;
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
            if (secureManagedBinding != null) {
                return secureManagedBinding.getBindAddress().getPort();
            }
            Integer securePort = securePortValue.getOptionalValue();
            if (securePort != null) {
//                return securePort;
            }
            return -1;
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

        public boolean hasConsole() {
            return consoleMode.hasConsole();
        }
    };

    public UndertowHttpManagementService(ConsoleMode consoleMode, String consoleSlot) {
        this.consoleMode = consoleMode;
        this.consoleSlot = consoleSlot;
    }

    /**
     * Starts the service.
     *
     * @param context The start context
     * @throws StartException If any errors occur
     */
    public synchronized void start(StartContext context) throws StartException {
        final ModelController modelController = modelControllerValue.getValue();
        final ControlledProcessStateService controlledProcessStateService = controlledProcessStateServiceValue.getValue();
        socketBindingManager = injectedSocketBindingManager.getOptionalValue();

        final SecurityRealm securityRealmService = securityRealmServiceValue.getOptionalValue();

        InetSocketAddress bindAddress = null;
        InetSocketAddress secureBindAddress = null;

        final SocketBinding basicBinding = injectedSocketBindingValue.getOptionalValue();
        final SocketBinding secureBinding = injectedSecureSocketBindingValue.getOptionalValue();
        final NetworkInterfaceBinding interfaceBinding = interfaceBindingValue.getOptionalValue();
        final NetworkInterfaceBinding secureInterfaceBinding = secureInterfaceBindingValue.getOptionalValue();
        if (interfaceBinding != null) {
            useUnmanagedBindings = true;
            final int port = portValue.getOptionalValue();
            if (port > 0) {
                bindAddress = new InetSocketAddress(interfaceBinding.getAddress(), port);
            }
            final int securePort = securePortValue.getOptionalValue();
            if (securePort > 0) {
                InetAddress secureAddress = secureInterfaceBinding == null ? interfaceBinding.getAddress() : secureInterfaceBinding.getAddress();
                secureBindAddress = new InetSocketAddress(secureAddress, securePort);
            }
        } else {
            if (basicBinding != null) {
                bindAddress = basicBinding.getSocketAddress();
            }
            if (secureBinding != null) {
                secureBindAddress = secureBinding.getSocketAddress();
            }
        }
        List<ListenerRegistry.Listener> listeners = new ArrayList<>();
        //TODO: rethink this whole ListenerRegistry business
        if(bindAddress != null) {
            ListenerRegistry.Listener http = new ListenerRegistry.Listener("http", HTTP_MANAGEMENT, SERVER_NAME, bindAddress);
            http.setContextInformation("socket-binding", basicBinding);
            listeners.add(http);
        }
        if(secureBindAddress != null) {
            ListenerRegistry.Listener https = new ListenerRegistry.Listener("https", HTTPS_MANAGEMENT, SERVER_NAME, bindAddress);
            https.setContextInformation("socket-binding", secureBinding);
            listeners.add(https);
        }

        final ChannelUpgradeHandler upgradeHandler = new ChannelUpgradeHandler();
        context.getChildTarget().addService(HTTP_UPGRADE_SERVICE_NAME, new ValueService<Object>(new ImmediateValue<Object>(upgradeHandler)))
                .addAliases(HTTPS_UPGRADE_SERVICE_NAME) //just to keep things consistent, should not be used for now
                .install();
        for (ListenerRegistry.Listener listener : listeners) {
            listener.addHttpUpgradeMetadata(new ListenerRegistry.HttpUpgradeMetadata(JBOSS_REMOTING, MANAGEMENT_ENDPOINT));
        }

        if(listenerRegistry.getOptionalValue() != null) {
            for(ListenerRegistry.Listener listener : listeners) {
                listenerRegistry.getOptionalValue().addListener(listener);
            }
        }

        try {

            serverManagement = ManagementHttpServer.create(bindAddress, secureBindAddress, 50, modelController,
                    securityRealmService, controlledProcessStateService, consoleMode, consoleSlot, upgradeHandler);

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
            throw ServerMessages.MESSAGES.failedToStartHttpManagementService(e);
        }
    }

    /**
     * Stops the service.
     *
     * @param context The stop context
     */
    public synchronized void stop(StopContext context) {
        ListenerRegistry lr = listenerRegistry.getOptionalValue();
        if(lr != null) {
            lr.removeListener(HTTP_MANAGEMENT);
            lr.removeListener(HTTPS_MANAGEMENT);
        }
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

    /**
     * Get the secure interface binding injector.
     *
     * @return The injector
     */
    public Injector<NetworkInterfaceBinding> getSecureInterfaceInjector() {
        return secureInterfaceBindingValue;
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
    public InjectedValue<SecurityRealm> getSecurityRealmInjector() {
        return securityRealmServiceValue;
    }

    /**
     * Get the security realm injector.
     *
     * @return the securityRealmServiceValue
     */
    public InjectedValue<ControlledProcessStateService> getControlledProcessStateServiceInjector() {
        return controlledProcessStateServiceValue;
    }

    public InjectedValue<ListenerRegistry> getListenerRegistry() {
        return listenerRegistry;
    }
}
