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

import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE_SECURITY_KEY;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.ADVERTISE_SOCKET;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.AUTO_ENABLE_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.BALANCER;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.EXCLUDED_CONTEXTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_PACKETS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.FLUSH_WAIT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.LOAD_BALANCING_GROUP;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.MAX_ATTEMPTS;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.NODE_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PING;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PROXIES;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.PROXY_URL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SESSION_DRAINING_STRATEGY;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SMAX;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SOCKET_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SSL_CONTEXT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.SSL_CONTEXT_CAPABILITY_NAME;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_FORCE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STICKY_SESSION_REMOVE;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.STOP_CONTEXT_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.TTL;
import static org.wildfly.extension.mod_cluster.ModClusterConfigResourceDefinition.WORKER_TIMEOUT;
import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CA_CERTIFICATE_FILE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CA_REVOCATION_URL;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CERTIFICATE_KEY_FILE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.CIPHER_SUITE;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.KEY_ALIAS;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PASSWORD;
import static org.wildfly.extension.mod_cluster.ModClusterSSLResourceDefinition.PROTOCOL;

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

import javax.net.ssl.SSLContext;

import org.jboss.as.clustering.controller.ResourceServiceBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.OutboundSocketBinding;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.modcluster.config.ModClusterConfiguration;
import org.jboss.modcluster.config.ProxyConfiguration;
import org.jboss.modcluster.config.builder.ModClusterConfigurationBuilder;
import org.jboss.modcluster.config.impl.ModClusterConfig;
import org.jboss.modcluster.config.impl.SessionDrainingStrategyEnum;
import org.jboss.modcluster.mcmp.impl.JSSESocketFactory;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.ValueService;
import org.jboss.msc.value.Value;
import org.wildfly.clustering.service.Builder;
import org.wildfly.clustering.service.InjectedValueDependency;
import org.wildfly.clustering.service.ValueDependency;

/**
 * @author Radoslav Husar
 */
public class ModClusterConfigurationServiceBuilder implements ResourceServiceBuilder<ModClusterConfiguration>, Value<ModClusterConfiguration> {

    public static final String SOCKET_BINDING_CAPABILITY_NAME = "org.wildfly.network.socket-binding";
    public static final String OUTBOUND_SOCKET_BINDING_CAPABILITY_NAME = "org.wildfly.network.outbound-socket-binding";

    private final ModClusterConfigurationBuilder builder = new ModClusterConfigurationBuilder();

    private ValueDependency<SocketBinding> advertiseSocketDependency = null;
    private Map<String, ValueDependency<OutboundSocketBinding>> outboundSocketBindings = null;
    private ValueDependency<SSLContext> sslContextDependency = null;

    @Override
    public ServiceName getServiceName() {
        return ContainerEventHandlerService.CONFIG_SERVICE_NAME;
    }

    @Override
    public Builder<ModClusterConfiguration> configure(OperationContext context, ModelNode model) throws OperationFailedException {

        // Advertise
        ModelNode advertiseSocketRef = ADVERTISE_SOCKET.resolveModelAttribute(context, model);
        if (advertiseSocketRef.isDefined()) {
            this.advertiseSocketDependency = new InjectedValueDependency<>(context.getCapabilityServiceName(SOCKET_BINDING_CAPABILITY_NAME, advertiseSocketRef.asString(), SocketBinding.class), SocketBinding.class);
        }

        if (model.hasDefined(CommonAttributes.ADVERTISE_SECURITY_KEY)) {
            builder.advertise().setAdvertiseSecurityKey(ADVERTISE_SECURITY_KEY.resolveModelAttribute(context, model).asString());
        }

        // MCMP

        builder.mcmp()
                .setAdvertise(ADVERTISE.resolveModelAttribute(context, model).asBoolean())
                .setProxyURL(PROXY_URL.resolveModelAttribute(context, model).asString())
                .setAutoEnableContexts(AUTO_ENABLE_CONTEXTS.resolveModelAttribute(context, model).asBoolean())
                .setStopContextTimeout(STOP_CONTEXT_TIMEOUT.resolveModelAttribute(context, model).asInt())
                .setStopContextTimeoutUnit(TimeUnit.valueOf(STOP_CONTEXT_TIMEOUT.getMeasurementUnit().getName()))
                .setSocketTimeout(SOCKET_TIMEOUT.resolveModelAttribute(context, model).asInt() * 1000)
                .setSessionDrainingStrategy(Enum.valueOf(SessionDrainingStrategyEnum.class, SESSION_DRAINING_STRATEGY.resolveModelAttribute(context, model).asString()))
        ;

        if (model.hasDefined(CommonAttributes.EXCLUDED_CONTEXTS)) {
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
                            throw new IllegalArgumentException(trimmedContexts + " is not a valid value for excludedContexts");
                        }

                        String host = null;
                        String trimmedContext = parts[0].trim();

                        if (parts.length == 2) {
                            host = trimmedContext;
                            trimmedContext = parts[1].trim();
                        }

                        String path = trimmedContext.equals("ROOT") ? "" : "/" + trimmedContext;

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
                .setWorkerTimeout(WORKER_TIMEOUT.resolveModelAttribute(context, model).asInt())
                .setMaxAttempts(MAX_ATTEMPTS.resolveModelAttribute(context, model).asInt())
        ;

        // Node

        builder.node()
                .setFlushPackets(FLUSH_PACKETS.resolveModelAttribute(context, model).asBoolean())
                .setFlushWait(FLUSH_WAIT.resolveModelAttribute(context, model).asInt())
                .setPing(PING.resolveModelAttribute(context, model).asInt())
                .setSmax(SMAX.resolveModelAttribute(context, model).asInt())
                .setTtl(TTL.resolveModelAttribute(context, model).asInt())
                .setNodeTimeout(NODE_TIMEOUT.resolveModelAttribute(context, model).asInt())
        ;
        if (model.hasDefined(CommonAttributes.BALANCER)) {
            builder.node().setBalancer(BALANCER.resolveModelAttribute(context, model).asString());
        }
        if (model.hasDefined(CommonAttributes.LOAD_BALANCING_GROUP)) {
            builder.node().setLoadBalancingGroup(LOAD_BALANCING_GROUP.resolveModelAttribute(context, model).asString());
        }


        ModelNode proxiesRef = PROXIES.resolveModelAttribute(context, model);
        if (proxiesRef.isDefined()) {
            outboundSocketBindings = new HashMap<>();
            proxiesRef.asList().stream()
                    .map(ModelNode::asString)
                    .forEach(ref -> outboundSocketBindings.put(ref, new InjectedValueDependency<>(context.getCapabilityServiceName(OUTBOUND_SOCKET_BINDING_CAPABILITY_NAME, ref, OutboundSocketBinding.class), OutboundSocketBinding.class)));
            this.advertiseSocketDependency = new InjectedValueDependency<>(context.getCapabilityServiceName(SOCKET_BINDING_CAPABILITY_NAME, advertiseSocketRef.asString(), SocketBinding.class), SocketBinding.class);
        }

        if (model.hasDefined(CommonAttributes.PROXY_LIST)) {
            throw new OperationFailedException(ROOT_LOGGER.proxyListNotAllowedInCurrentModel());
        }


        // Elytron-based security support

        ModelNode sslContext = SSL_CONTEXT.resolveModelAttribute(context, model);
        if (sslContext.isDefined()) {
            this.sslContextDependency = new InjectedValueDependency<>(context.getCapabilityServiceName(SSL_CONTEXT_CAPABILITY_NAME, sslContext.asString(), SSLContext.class), SSLContext.class);
        }

        // Legacy security support

        if (model.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair()).isDefined()) {
            final ModelNode ssl = model.get(ModClusterSSLResourceDefinition.PATH.getKeyValuePair());
            ModelNode keyAlias = KEY_ALIAS.resolveModelAttribute(context, ssl);
            ModelNode password = PASSWORD.resolveModelAttribute(context, ssl);

            final ModClusterConfig sslConfiguration = new ModClusterConfig();

            if (keyAlias.isDefined()) {
                sslConfiguration.setSslKeyAlias(keyAlias.asString());
            }
            if (password.isDefined()) {
                sslConfiguration.setSslTrustStorePassword(password.asString());
                sslConfiguration.setSslKeyStorePassword(password.asString());
            }
            if (ssl.hasDefined(CommonAttributes.CERTIFICATE_KEY_FILE)) {
                sslConfiguration.setSslKeyStore(CERTIFICATE_KEY_FILE.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.CIPHER_SUITE)) {
                sslConfiguration.setSslCiphers(CIPHER_SUITE.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.PROTOCOL)) {
                sslConfiguration.setSslProtocol(PROTOCOL.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.CA_CERTIFICATE_FILE)) {
                sslConfiguration.setSslTrustStore(CA_CERTIFICATE_FILE.resolveModelAttribute(context, ssl).asString());
            }
            if (ssl.hasDefined(CommonAttributes.CA_REVOCATION_URL)) {
                sslConfiguration.setSslCrlFile(CA_REVOCATION_URL.resolveModelAttribute(context, ssl).asString());
            }

            builder.mcmp().setSocketFactory(new JSSESocketFactory(sslConfiguration));
        }

        return this;
    }

    @Override
    public ServiceBuilder<ModClusterConfiguration> build(ServiceTarget target) {
        ServiceBuilder<ModClusterConfiguration> builder = target.addService(this.getServiceName(), new ValueService<>(this));

        if (advertiseSocketDependency != null) {
            this.advertiseSocketDependency.register(builder);
        }

        if (outboundSocketBindings != null) {
            outboundSocketBindings.values().forEach(binding -> binding.register(builder));
        }

        if (sslContextDependency != null) {
            this.sslContextDependency.register(builder);
        }

        builder.setInitialMode(ServiceController.Mode.PASSIVE);

        return builder;
    }

    @Override
    public ModClusterConfiguration getValue() throws IllegalStateException, IllegalArgumentException {

        // Advertise
        if (advertiseSocketDependency != null) {
            final SocketBinding binding = advertiseSocketDependency.getValue();
            builder.advertise()
                    .setAdvertiseSocketAddress(binding.getMulticastSocketAddress())
                    .setAdvertiseInterface(binding.getSocketAddress().getAddress())
            ;
            if (!isMulticastEnabled(binding.getSocketBindings().getDefaultInterfaceBinding().getNetworkInterfaces())) {
                ROOT_LOGGER.multicastInterfaceNotAvailable();
            }
        }

        // Proxies
        if (outboundSocketBindings != null) {
            List<ProxyConfiguration> proxies = new LinkedList<>();
            for (final ValueDependency<OutboundSocketBinding> outboundSocketBindingValueDependency : outboundSocketBindings.values()) {
                OutboundSocketBinding binding = outboundSocketBindingValueDependency.getValue();
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
            builder.mcmp().setProxyConfigurations(proxies);
        }

        // SSL
        if (sslContextDependency != null) {
            builder.mcmp().setSocketFactory(sslContextDependency.getValue().getSocketFactory());
        }

        return builder.build();
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
