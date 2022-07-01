/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.ADVERTISE;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.ADVERTISE_SECURITY_KEY;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.ADVERTISE_SOCKET;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.AUTO_ENABLE_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.BALANCER;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.EXCLUDED_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.FLUSH_PACKETS;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.FLUSH_WAIT;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.LOAD_BALANCING_GROUP;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.MAX_ATTEMPTS;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.NODE_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.PING;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.PROXIES;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.PROXY_URL;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.SESSION_DRAINING_STRATEGY;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.SMAX;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.SOCKET_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.SSL_CONTEXT;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION_FORCE;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.STICKY_SESSION_REMOVE;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.STOP_CONTEXT_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.TTL;
import static org.wildfly.extension.mod_cluster.ProxyConfigurationResourceDefinition.Attribute.WORKER_TIMEOUT;

import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.CapabilityServiceNameProvider;
import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.network.ManagedBinding;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.builder.ModClusterConfigurationBuilder;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.msc.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.Dependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;

/**
 * @author Radoslav Husar
 */
public class ProxyConfigurationServiceConfigurator extends CapabilityServiceNameProvider implements ResourceServiceConfigurator, Supplier<ModClusterConfiguration>, Consumer<ModClusterConfiguration> {

    private volatile SupplierDependency<SocketBinding> advertiseSocketDependency = null;
    private final List<SupplierDependency<OutboundSocketBinding>> outboundSocketBindings = new LinkedList<>();
    private volatile SupplierDependency<SSLContext> sslContextDependency = null;

    private final ModClusterConfigurationBuilder builder = new ModClusterConfigurationBuilder();

    ProxyConfigurationServiceConfigurator(PathAddress address) {
        super(ProxyConfigurationResourceDefinition.Capability.SERVICE, address);
    }

    @Override
    public ServiceName getServiceName() {
        return super.getServiceName().append("configuration");
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {

        // Advertise
        String advertiseSocket = ADVERTISE_SOCKET.resolveModelAttribute(context, model).asStringOrNull();
        this.advertiseSocketDependency = (advertiseSocket != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, advertiseSocket)) : null;
        this.builder.advertise().setAdvertiseSecurityKey(ADVERTISE_SECURITY_KEY.resolveModelAttribute(context, model).asStringOrNull());

        // MCMP

        builder.mcmp()
                .setAdvertise(ADVERTISE.resolveModelAttribute(context, model).asBoolean())
                .setProxyURL(PROXY_URL.resolveModelAttribute(context, model).asString())
                .setAutoEnableContexts(AUTO_ENABLE_CONTEXTS.resolveModelAttribute(context, model).asBoolean())
                .setStopContextTimeout(STOP_CONTEXT_TIMEOUT.resolveModelAttribute(context, model).asInt())
                .setStopContextTimeoutUnit(TimeUnit.valueOf(STOP_CONTEXT_TIMEOUT.getDefinition().getMeasurementUnit().getName()))
                .setSocketTimeout(SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt() * 1000)
                .setSessionDrainingStrategy(Enum.valueOf(SessionDrainingStrategyEnum.class, SESSION_DRAINING_STRATEGY.resolveModelAttribute(context, model).asString()))
        ;

        if (model.hasDefined(EXCLUDED_CONTEXTS.getName())) {
            String contexts = EXCLUDED_CONTEXTS.resolveModelAttribute(context, model).asString();
            Map<String, Set<String>> excludedContextsPerHost;
            if (contexts == null) {
                excludedContextsPerHost = Collections.emptyMap();
            } else {
                String trimmedContexts = contexts.trim();

                if (trimmedContexts.isEmpty()) {
                    excludedContextsPerHost = Collections.emptyMap();
                } else {
                    excludedContextsPerHost = new HashMap<>();

                    for (String c : trimmedContexts.split(",")) {
                        String[] parts = c.trim().split(":");

                        if (parts.length > 2) {
                            throw ROOT_LOGGER.excludedContextsWrongFormat(trimmedContexts);
                        }

                        String host = null;
                        String trimmedContext = parts[0].trim();

                        if (parts.length == 2) {
                            host = trimmedContext;
                            trimmedContext = parts[1].trim();
                        }

                        String path;
                        switch (trimmedContext) {
                            case "ROOT":
                                ROOT_LOGGER.excludedContextsUseSlashInsteadROOT();
                            case "/":
                                path = "";
                                break;
                            default:
                                // normalize the context by pre-pending or removing trailing slash
                                trimmedContext = trimmedContext.startsWith("/") ? trimmedContext : ("/" + trimmedContext);
                                path = trimmedContext.endsWith("/") ? trimmedContext.substring(0, trimmedContext.length() - 1) : trimmedContext;
                                break;
                        }

                        Set<String> paths = excludedContextsPerHost.computeIfAbsent(host, k -> new HashSet<>());

                        paths.add(path);
                    }
                }
            }
            builder.mcmp().setExcludedContextsPerHost(excludedContextsPerHost);
        }

        // Balancer

        builder.balancer()
                .setStickySession(STICKY_SESSION.resolveModelAttribute(context, model).asBoolean())
                .setStickySessionRemove(STICKY_SESSION_REMOVE.resolveModelAttribute(context, model).asBoolean())
                .setStickySessionForce(STICKY_SESSION_FORCE.resolveModelAttribute(context, model).asBoolean())
                .setMaxAttempts(MAX_ATTEMPTS.resolveModelAttribute(context, model).asInt())
        ;

        ModelNode node = WORKER_TIMEOUT.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.balancer().setWorkerTimeout(node.asInt());
        }

        // Node

        builder.node()
                .setFlushPackets(FLUSH_PACKETS.resolveModelAttribute(context, model).asBoolean())
                .setPing(PING.resolveModelAttribute(context, model).asInt())
        ;

        node = FLUSH_WAIT.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setFlushWait(node.asInt());
        }
        node = SMAX.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setSmax(node.asInt());
        }
        node = TTL.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setTtl(node.asInt());
        }
        node = NODE_TIMEOUT.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setNodeTimeout(node.asInt());
        }
        node = BALANCER.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setBalancer(node.asString());
        }
        node = LOAD_BALANCING_GROUP.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            builder.node().setLoadBalancingGroup(node.asString());
        }
        node = PROXIES.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            for (ModelNode ref : node.asList()) {
                String asString = ref.asString();
                this.outboundSocketBindings.add(new ServiceSupplierDependency<>(CommonUnaryRequirement.OUTBOUND_SOCKET_BINDING.getServiceName(context, asString)));
            }
        }

        // Elytron-based security support

        node = SSL_CONTEXT.resolveModelAttribute(context, model);
        if (node.isDefined()) {
            this.sslContextDependency = new ServiceSupplierDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, node.asString()));
        }

        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceBuilder<?> builder = target.addService(this.getServiceName());
        Consumer<ModClusterConfiguration> config = new CompositeDependency(this.advertiseSocketDependency, this.sslContextDependency).register(builder).provides(this.getServiceName());
        for (Dependency dependency : this.outboundSocketBindings) {
            dependency.register(builder);
        }
        Service service = new FunctionalService<>(config, Function.identity(), this, this);
        return builder.setInstance(service).setInitialMode(ServiceController.Mode.PASSIVE);
    }

    @Override
    public ModClusterConfiguration get() {

        // Advertise
        if (advertiseSocketDependency != null) {
            final SocketBinding binding = advertiseSocketDependency.get();
            builder.advertise()
                    .setAdvertiseSocketAddress(binding.getMulticastSocketAddress())
                    .setAdvertiseInterface(binding.getNetworkInterfaceBinding().getNetworkInterfaces().stream().findFirst().orElse(null))
            ;

            // Register the binding with named registry as bound (WFLY-11447)
            ManagedBinding simpleManagedBinding = ManagedBinding.Factory.createSimpleManagedBinding(binding);
            binding.getSocketBindings().getNamedRegistry().registerBinding(simpleManagedBinding);

            if (!isMulticastEnabled(binding.getSocketBindings().getDefaultInterfaceBinding().getNetworkInterfaces())) {
                ROOT_LOGGER.multicastInterfaceNotAvailable();
            }
        }

        // Proxies
        List<ProxyConfiguration> proxies = new LinkedList<>();
        for (final Supplier<OutboundSocketBinding> outboundSocketBindingValueDependency : outboundSocketBindings) {
            OutboundSocketBinding binding = outboundSocketBindingValueDependency.get();
            proxies.add(new ProxyConfiguration() {

                @Override
                public InetSocketAddress getRemoteAddress() {
                    // Both host and port may not be null in the model, no need to validate here
                    // Don't do resolving here, let mod_cluster deal with it
                    return new InetSocketAddress(binding.getUnresolvedDestinationAddress(), binding.getDestinationPort());
                }

                @Override
                public InetSocketAddress getLocalAddress() {
                    if (binding.getOptionalSourceAddress() != null) {
                        return new InetSocketAddress(binding.getOptionalSourceAddress(), binding.getAbsoluteSourcePort() == null ? 0 : binding.getAbsoluteSourcePort());
                    } else if (binding.getAbsoluteSourcePort() != null) {
                        // Bind to port only if source address is not configured
                        return new InetSocketAddress(binding.getAbsoluteSourcePort());
                    }
                    // No binding configured so don't bind
                    return null;
                }

            });
        }
        if (!proxies.isEmpty()) {
            builder.mcmp().setProxyConfigurations(proxies);
        }

        // SSL
        if (sslContextDependency != null) {
            builder.mcmp().setSocketFactory(sslContextDependency.get().getSocketFactory());
        }

        return builder.build();
    }

    // FunctionalService#destroyer implementation
    @Override
    public void accept(ModClusterConfiguration modClusterConfiguration) {
        if (advertiseSocketDependency != null) {
            SocketBinding binding = advertiseSocketDependency.get();
            ManagedBinding simpleManagedBinding = ManagedBinding.Factory.createSimpleManagedBinding(binding);
            binding.getSocketBindings().getNamedRegistry().unregisterBinding(simpleManagedBinding);
        }
    }

    private static boolean isMulticastEnabled(Collection<NetworkInterface> interfaces) {
        for (NetworkInterface iface : interfaces) {
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

}
