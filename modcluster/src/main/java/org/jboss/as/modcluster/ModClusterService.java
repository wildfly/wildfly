/*
 * JBoss, Home of Professional Open Source
 * Copyright 2011, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.modcluster;

import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Engine;
import org.apache.catalina.connector.Connector;
import org.jboss.as.clustering.msc.AsynchronousService;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.web.WebServer;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.container.catalina.CatalinaFactory;
import org.jboss.modcluster.container.catalina.ProxyConnectorProvider;
import org.jboss.modcluster.container.catalina.ServerProvider;
import org.jboss.modcluster.container.catalina.ServiceLoaderCatalinaFactory;
import org.jboss.modcluster.container.catalina.SimpleProxyConnectorProvider;
import org.jboss.modcluster.container.catalina.SimpleServerProvider;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.InjectedValue;

/**
 * Service configuring and starting modcluster.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterService extends AsynchronousService<ModCluster> implements ModCluster, Service<ModCluster> {

    static final ServiceName NAME = ServiceName.JBOSS.append("mod-cluster");

    private CatalinaEventHandlerAdapter adapter;
    private LoadBalanceFactorProvider load;
    private ModClusterConfig config;

    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();
    private final InjectedValue<SocketBindingManager> bindingManager = new InjectedValue<SocketBindingManager>();
    private final InjectedValue<SocketBinding> binding = new InjectedValue<SocketBinding>();
    private final InjectedValue<Connector> connector = new InjectedValue<Connector>();

    /* Depending on configuration we use one of the other */
    private org.jboss.modcluster.ModClusterService service;

    ModClusterService(ModClusterConfig config, LoadBalanceFactorProvider load) {
        super(true, true);
        this.config = config;
        this.load = load;
    }

    @Override
    protected void start() {
        ROOT_LOGGER.debugf("Starting Mod_cluster Extension");

        boolean isMulticast = isMulticastEnabled(bindingManager.getValue().getDefaultInterfaceBinding().getNetworkInterfaces());

        // Set some defaults...
        if (config.getProxies().isEmpty()) {
            config.setAdvertise(isMulticast);
        }

        // Read node to set configuration.
        if (config.getAdvertise()) {
            // There should be a socket-binding....
            final SocketBinding binding = this.binding.getValue();
            if (binding != null) {
                config.setAdvertiseSocketAddress(binding.getMulticastSocketAddress());
                config.setAdvertiseInterface(binding.getSocketAddress().getAddress());
                if (!isMulticast) {
                    ROOT_LOGGER.multicastInterfaceNotAvailable();
                }
            }
        }

        service = new org.jboss.modcluster.ModClusterService(config, load);

        WebServer webServer = this.webServer.getValue();
        ServerProvider serverProvider = new SimpleServerProvider(webServer.getServer());
        ProxyConnectorProvider connectorProvider = new SimpleProxyConnectorProvider(connector.getValue());
        CatalinaFactory factory = new ServiceLoaderCatalinaFactory(connectorProvider);

        adapter = new CatalinaEventHandlerAdapter(service, serverProvider, factory);
        adapter.start();

        // Trigger a manual STATUS now instead of waiting potentially 10 seconds
        Engine engine = (Engine) webServer.getService().getContainer();
        service.status(factory.createEngine(engine));
    }

    private boolean isMulticastEnabled(Collection<NetworkInterface> ifaces) {
        for (NetworkInterface iface : ifaces) {
            try {
                if (iface.isUp() && (iface.supportsMulticast() || iface.isLoopback())) {
                    return true;
                }
            } catch (SocketException e) {
                // Ignore
            }
        }
        return false;
    }

    @Override
    protected void stop() {
        if (adapter != null) {
            adapter.stop();
            adapter = null;
        }
    }

    @Override
    public synchronized ModCluster getValue() {
        return this;
    }


    public Injector<WebServer> getWebServer() {
        return webServer;
    }

    public Injector<SocketBinding> getBinding() {
        return binding;
    }

    public Injector<SocketBindingManager> getBindingManager() {
        return bindingManager;
    }

    public Injector<Connector> getConnectorInjector() {
        return connector;
    }

    @Override
    public Map<InetSocketAddress, String> getProxyInfo() {
        return service.getProxyInfo();
    }

    @Override
    public void refresh() {
        service.refresh();
    }

    @Override
    public void reset() {
        service.reset();
    }

    @Override
    public void enable() {
        service.enable();
    }

    @Override
    public void disable() {
        service.disable();
    }

    @Override
    public void stop(int waittime) {
        service.stop(waittime, TimeUnit.SECONDS);
    }

    @Override
    public boolean enableContext(String host, String context) {
        return service.enableContext(host, context);
    }

    @Override
    public boolean disableContext(String host, String context) {
        return service.disableContext(host, context);
    }

    @Override
    public boolean stopContext(String host, String context, int waittime) {
        return service.stopContext(host, context, waittime, TimeUnit.SECONDS);
    }

    @Override
    public void addProxy(String host, int port) {
        service.addProxy(host, port);
    }

    @Override
    public void removeProxy(String host, int port) {
        service.removeProxy(host, port);
    }

    @Override
    public Map<InetSocketAddress, String> getProxyConfiguration() {
        return service.getProxyConfiguration();
    }
}
