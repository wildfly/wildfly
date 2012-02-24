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
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.catalina.Engine;
import org.jboss.as.network.SocketBinding;
import org.jboss.as.network.SocketBindingManager;
import org.jboss.as.web.WebServer;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.container.catalina.CatalinaEventHandlerAdapter;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.load.LoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.DynamicLoadBalanceFactorProvider;
import org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider;
import org.jboss.modcluster.load.metric.LoadMetric;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

/**
 * Service configuring and starting modcluster.
 *
 * @author Jean-Frederic Clere
 */
class ModClusterService implements ModCluster, Service<ModCluster> {

    static final ServiceName NAME = ServiceName.JBOSS.append("mod-cluster");

    private final String unmaskedPassword;
    private final ModelNode modelconf;

    private CatalinaEventHandlerAdapter adapter;
    private LoadBalanceFactorProvider load;

    private final InjectedValue<WebServer> webServer = new InjectedValue<WebServer>();
    private final InjectedValue<SocketBindingManager> bindingManager = new InjectedValue<SocketBindingManager>();
    private final InjectedValue<SocketBinding> binding = new InjectedValue<SocketBinding>();

    /* Depending on configuration we use one of the other */
    private org.jboss.modcluster.ModClusterService service;
    private ModClusterConfig config;
    public ModClusterService(final String unmaskedPassword, final ModelNode modelconf) {
        this.unmaskedPassword = unmaskedPassword;
        this.modelconf = modelconf;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void start(StartContext context) throws StartException {
        ROOT_LOGGER.debugf("Starting Mod_cluster Extension");

        config = new ModClusterConfig();
        // Set the configuration.

        // Set some defaults...
        if (!modelconf.hasDefined(CommonAttributes.PROXY_LIST)) {
            try {
                config.setAdvertise(this.isMulticastEnabled(Collections.list(NetworkInterface.getNetworkInterfaces())));
            } catch (SocketException e) {
                // Ignore
            }
        }
        config.setAdvertisePort(23364);
        config.setAdvertiseGroupAddress("224.0.1.105");
        config.setAdvertiseInterface(bindingManager.getValue().getDefaultInterfaceAddress().getHostAddress());
        config.setAutoEnableContexts(true);
        config.setStopContextTimeout(10);
        config.setSocketTimeout(20000);
        config.setExcludedContexts("ROOT,invoker,jbossws,juddi,console");

        // Read node to set configuration.
        if (modelconf.hasDefined(CommonAttributes.ADVERTISE_SOCKET)) {
            // There should be a socket-binding....
            final SocketBinding binding = this.binding.getValue();
            if (binding != null) {
                config.setAdvertisePort(binding.getMulticastPort());
                config.setAdvertiseGroupAddress(binding.getMulticastSocketAddress().getHostName());
                config.setAdvertiseInterface(binding.getSocketAddress().getAddress().getHostAddress());
                if (!this.isMulticastEnabled(binding.getNetworkInterfaceBinding().getNetworkInterfaces())) {
                    ROOT_LOGGER.multicastInterfaceNotAvailable();
                }
                config.setAdvertise(true);
            }
        }
        if (modelconf.hasDefined(CommonAttributes.SSL)) {
            // Add SSL configuration.
            config.setSsl(true);
            final ModelNode ssl = modelconf.get(CommonAttributes.SSL);
            if (ssl.has(CommonAttributes.KEY_ALIAS))
                config.setSslKeyAlias(ssl.get(CommonAttributes.KEY_ALIAS).asString());
            if (ssl.has(CommonAttributes.PASSWORD)) {
                config.setSslTrustStorePassword(unmaskedPassword);
                config.setSslKeyStorePassword(unmaskedPassword);
            }
            if (ssl.has(CommonAttributes.CERTIFICATE_KEY_FILE))
                config.setSslKeyStore(ssl.get(CommonAttributes.CERTIFICATE_KEY_FILE).asString());
            if (ssl.has(CommonAttributes.CIPHER_SUITE))
                config.setSslCiphers(ssl.get(CommonAttributes.CIPHER_SUITE).asString());
            if (ssl.has(CommonAttributes.PROTOCOL))
                config.setSslProtocol(ssl.get(CommonAttributes.PROTOCOL).asString());
            if (ssl.has(CommonAttributes.CA_CERTIFICATE_FILE))
                config.setSslTrustStore(ssl.get(CommonAttributes.CA_CERTIFICATE_FILE).asString());
            if (ssl.has(CommonAttributes.CA_REVOCATION_URL))
                config.setSslCrlFile(ssl.get(CommonAttributes.CA_REVOCATION_URL).asString());
        }
        if (modelconf.hasDefined(CommonAttributes.ADVERTISE))
            config.setAdvertise(modelconf.get(CommonAttributes.ADVERTISE).asBoolean());
        if (modelconf.hasDefined(CommonAttributes.PROXY_LIST)) {
            config.setProxyList(modelconf.get(CommonAttributes.PROXY_LIST).asString());
        }
        if (modelconf.hasDefined(CommonAttributes.PROXY_URL))
            config.setProxyURL(modelconf.get(CommonAttributes.PROXY_URL).asString());
        if (modelconf.has(CommonAttributes.ADVERTISE_SECURITY_KEY))
            config.setAdvertiseSecurityKey(modelconf.get(CommonAttributes.ADVERTISE_SECURITY_KEY).asString());

        if (modelconf.hasDefined(CommonAttributes.EXCLUDED_CONTEXTS)) {
            // read the default host.
            String defaulthost = ((Engine) webServer.getValue().getService().getContainer()).getDefaultHost();
            String exluded_contexts = null;
            for (String exluded_context : modelconf.get(CommonAttributes.EXCLUDED_CONTEXTS).asString().trim().split(",")) {
                String[] parts = exluded_context.trim().split(":");
                if (parts.length != 1) {
                    if (exluded_contexts == null)
                        exluded_contexts = "";
                    else
                        exluded_contexts = exluded_contexts.concat(",");
                    exluded_contexts = exluded_contexts.concat(exluded_context);
                } else {
                        if (exluded_contexts == null)
                            exluded_contexts = "";
                        else
                            exluded_contexts = exluded_contexts.concat(",");
                        exluded_contexts = exluded_contexts.concat(defaulthost).concat(":").concat(exluded_context);
                }
            }
            config.setExcludedContexts(exluded_contexts);
        }
        if (modelconf.hasDefined(CommonAttributes.AUTO_ENABLE_CONTEXTS))
            config.setAutoEnableContexts(modelconf.get(CommonAttributes.AUTO_ENABLE_CONTEXTS).asBoolean());
        if (modelconf.hasDefined(CommonAttributes.STOP_CONTEXT_TIMEOUT)) {
            config.setStopContextTimeout(modelconf.get(CommonAttributes.STOP_CONTEXT_TIMEOUT).asInt());
            config.setStopContextTimeoutUnit(TimeUnit.SECONDS);
        }
        if (modelconf.hasDefined(CommonAttributes.SOCKET_TIMEOUT)) {
            // the default value is 20000 = 20 seconds.
            config.setSocketTimeout(modelconf.get(CommonAttributes.SOCKET_TIMEOUT).asInt() * 1000);
        }

        if (modelconf.hasDefined(CommonAttributes.STICKY_SESSION))
            config.setStickySession(modelconf.get(CommonAttributes.STICKY_SESSION).asBoolean());
        if (modelconf.hasDefined(CommonAttributes.STICKY_SESSION_REMOVE))
            config.setStickySessionRemove(modelconf.get(CommonAttributes.STICKY_SESSION_REMOVE).asBoolean());
        if (modelconf.hasDefined(CommonAttributes.STICKY_SESSION_FORCE))
            config.setStickySessionForce(modelconf.get(CommonAttributes.STICKY_SESSION_FORCE).asBoolean());
        if (modelconf.hasDefined(CommonAttributes.WORKER_TIMEOUT))
            config.setWorkerTimeout(modelconf.get(CommonAttributes.WORKER_TIMEOUT).asInt());
        if (modelconf.hasDefined(CommonAttributes.MAX_ATTEMPTS))
            config.setMaxAttempts(modelconf.get(CommonAttributes.MAX_ATTEMPTS).asInt());
        if (modelconf.hasDefined(CommonAttributes.FLUSH_PACKETS))
            config.setFlushPackets(modelconf.get(CommonAttributes.FLUSH_PACKETS).asBoolean());
        if (modelconf.hasDefined(CommonAttributes.FLUSH_WAIT))
            config.setFlushWait(modelconf.get(CommonAttributes.FLUSH_WAIT).asInt());
        if (modelconf.hasDefined(CommonAttributes.PING))
            config.setPing(modelconf.get(CommonAttributes.PING).asInt());
        if (modelconf.hasDefined(CommonAttributes.SMAX))
            config.setSmax(modelconf.get(CommonAttributes.SMAX).asInt());
        if (modelconf.hasDefined(CommonAttributes.TTL))
            config.setTtl(modelconf.get(CommonAttributes.TTL).asInt());
        if (modelconf.hasDefined(CommonAttributes.NODE_TIMEOUT))
            config.setNodeTimeout(modelconf.get(CommonAttributes.NODE_TIMEOUT).asInt());
        if (modelconf.hasDefined(CommonAttributes.BALANCER))
            config.setBalancer(modelconf.get(CommonAttributes.BALANCER).asString());
        if (modelconf.hasDefined(CommonAttributes.DOMAIN))
            config.setLoadBalancingGroup(modelconf.get(CommonAttributes.DOMAIN).asString());

        if (modelconf.hasDefined(CommonAttributes.SIMPLE_LOAD_PROVIDER)) {
            // TODO it seems we don't support that stuff.
            // LoadBalanceFactorProvider implementation, org.jboss.modcluster.load.impl.SimpleLoadBalanceFactorProvider.
            final ModelNode node = modelconf.get(CommonAttributes.SIMPLE_LOAD_PROVIDER);
            SimpleLoadBalanceFactorProvider myload = new SimpleLoadBalanceFactorProvider();
            myload.setLoadBalanceFactor(node.get(CommonAttributes.FACTOR).asInt(1));
            load = myload;
        }

        Set<LoadMetric> metrics = new HashSet<LoadMetric>();
        if (modelconf.hasDefined(CommonAttributes.DYNAMIC_LOAD_PROVIDER)) {
            final ModelNode node = modelconf.get(CommonAttributes.DYNAMIC_LOAD_PROVIDER);
            int decayFactor = node.get(CommonAttributes.DECAY).asInt(DynamicLoadBalanceFactorProvider.DEFAULT_DECAY_FACTOR);
            int history = node.get(CommonAttributes.HISTORY).asInt(DynamicLoadBalanceFactorProvider.DEFAULT_HISTORY);
            // We should have bunch of load-metric and/or custom-load-metric here.
            // TODO read the child nodes or what ....String nodes = node.
            if (node.hasDefined(CommonAttributes.LOAD_METRIC)) {
                addLoadMetrics(metrics, node.get(CommonAttributes.LOAD_METRIC));
             }
            if (node.hasDefined(CommonAttributes.CUSTOM_LOAD_METRIC)) {
                addLoadMetrics(metrics, node.get(CommonAttributes.CUSTOM_LOAD_METRIC));
            }
            if (!metrics.isEmpty()) {
                DynamicLoadBalanceFactorProvider loader = new DynamicLoadBalanceFactorProvider(metrics);
                loader.setDecayFactor(decayFactor);
                loader.setHistory(history);
                load = loader;
            }
        }

        if (load == null) {
            // Use a default one...
            ROOT_LOGGER.useDefaultLoadBalancer();
            SimpleLoadBalanceFactorProvider myload = new SimpleLoadBalanceFactorProvider();
            myload.setLoadBalanceFactor(1);
            load = myload;
        }
        service = new org.jboss.modcluster.ModClusterService(config, load);
        adapter = new CatalinaEventHandlerAdapter(service, webServer.getValue().getServer());
        adapter.start();
    }

    private boolean isMulticastEnabled(Collection<NetworkInterface> ifaces) {
        for (NetworkInterface iface: ifaces) {
            try {
                if (iface.isUp() && iface.supportsMulticast()) {
                    return true;
                }
            } catch (SocketException e) {
                // Ignore
            }
        }
        return false;
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void stop(StopContext context) {
        if (adapter != null) {
            adapter.stop();
            adapter = null;
        }
    }

    @Override
    public synchronized ModCluster getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    private void addLoadMetrics(Set<LoadMetric> metrics, ModelNode nodes) {
        for (ModelNode node: nodes.asList()) {
            double capacity = node.get(CommonAttributes.CAPACITY).asDouble(LoadMetric.DEFAULT_CAPACITY);
            int weight = node.get(CommonAttributes.WEIGHT).asInt(LoadMetric.DEFAULT_WEIGHT);
            Class<? extends LoadMetric> loadMetricClass = null;
            if (node.hasDefined(CommonAttributes.TYPE)) {
                String type = node.get(CommonAttributes.TYPE).asString();
                LoadMetricEnum metric = LoadMetricEnum.forType(type);
                loadMetricClass = (metric != null) ? metric.getLoadMetricClass() : null;
            } else {
                String className = node.get(CommonAttributes.CLASS).asString();
                try {
                    loadMetricClass = this.getClass().getClassLoader().loadClass(className).asSubclass(LoadMetric.class);
                } catch (ClassNotFoundException e) {
                    ROOT_LOGGER.errorAddingMetrics(e);
                }
            }

            if (loadMetricClass != null) {
                try {
                    LoadMetric metric = loadMetricClass.newInstance();
                    metric.setCapacity(capacity);
                    metric.setWeight(weight);
                    metrics.add(metric);
                } catch (InstantiationException e) {
                    ROOT_LOGGER.errorAddingMetrics(e);
                } catch (IllegalAccessException e) {
                    ROOT_LOGGER.errorAddingMetrics(e);
                }
            }
        }
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
