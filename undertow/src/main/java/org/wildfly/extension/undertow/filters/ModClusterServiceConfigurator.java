/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.filters;

import static org.wildfly.extension.undertow.logging.UndertowLogger.ROOT_LOGGER;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.SSLContext;

import io.undertow.UndertowOptions;
import io.undertow.client.UndertowClient;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import io.undertow.server.handlers.proxy.RouteParsingStrategy;
import io.undertow.server.handlers.proxy.mod_cluster.MCMPConfig;
import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;

import org.jboss.as.clustering.controller.CommonUnaryRequirement;
import org.jboss.as.clustering.controller.ResourceServiceConfigurator;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.wildfly.clustering.service.CompositeDependency;
import org.wildfly.clustering.service.FunctionalService;
import org.wildfly.clustering.service.ServiceConfigurator;
import org.wildfly.clustering.service.ServiceSupplierDependency;
import org.wildfly.clustering.service.SupplierDependency;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.XnioWorker;
import org.xnio.ssl.XnioSsl;

/**
 * Configures a service that provides both {@link ModCluster} and {@link MCMPConfig}.
 * @author Paul Ferraro
 */
public class ModClusterServiceConfigurator extends ModClusterServiceNameProvider implements ResourceServiceConfigurator, Supplier<Map.Entry<ModCluster, MCMPConfig>>, Consumer<Map.Entry<ModCluster, MCMPConfig>> {

    private static final Map<PathElement, RouteParsingStrategy> ROUTE_PARSING_STRATEGIES = Map.of(
            NoAffinityResourceDefinition.PATH, RouteParsingStrategy.NONE,
            SingleAffinityResourceDefinition.PATH, RouteParsingStrategy.SINGLE,
            RankedAffinityResourceDefinition.PATH, RouteParsingStrategy.RANKED);

    private volatile SupplierDependency<XnioWorker> worker;
    private volatile SupplierDependency<SocketBinding> managementBinding;
    private volatile SupplierDependency<SocketBinding> advertiseBinding;
    private volatile SupplierDependency<SSLContext> sslContext;

    private volatile OptionMap clientOptions;
    private volatile RouteParsingStrategy routeParsingStrategy;
    private volatile String routeDelimiter;
    private volatile long healthCheckInterval;
    private volatile int maxRequestTime;
    private volatile long brokenNodeTimeout;
    private volatile int advertiseFrequency;
    private volatile String advertisePath;
    private volatile String advertiseProtocol;
    private volatile String securityKey;
    private volatile int maxConnections;
    private volatile int cachedConnections;
    private volatile int connectionIdleTimeout;
    private volatile int requestQueueSize;
    private volatile boolean useAlias;
    private volatile int maxRetries;
    private volatile FailoverStrategy failoverStrategy;

    ModClusterServiceConfigurator(PathAddress address) {
        super(address);
    }

    ServiceName getConfigServiceName() {
        return this.getServiceName().append("config");
    }

    @Override
    public ServiceConfigurator configure(OperationContext context, ModelNode model) throws OperationFailedException {

        if (ModClusterDefinition.SECURITY_REALM.resolveModelAttribute(context, model).isDefined()) {
             ROOT_LOGGER.runtimeSecurityRealmUnsupported();
        }

        String sslContext = ModClusterDefinition.SSL_CONTEXT.resolveModelAttribute(context, model).asStringOrNull();
        this.sslContext = (sslContext != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SSL_CONTEXT.getServiceName(context, sslContext)) : null;

        OptionMap.Builder builder = OptionMap.builder();
        Integer packetSize = ModClusterDefinition.MAX_AJP_PACKET_SIZE.resolveModelAttribute(context, model).asIntOrNull();
        if (packetSize != null) {
            builder.set(UndertowOptions.MAX_AJP_PACKET_SIZE, packetSize);
        }
        builder.set(UndertowOptions.ENABLE_HTTP2, ModClusterDefinition.ENABLE_HTTP2.resolveModelAttribute(context, model).asBoolean());
        ModClusterDefinition.HTTP2_ENABLE_PUSH.resolveOption(context, model, builder);
        ModClusterDefinition.HTTP2_HEADER_TABLE_SIZE.resolveOption(context, model, builder);
        ModClusterDefinition.HTTP2_INITIAL_WINDOW_SIZE.resolveOption(context, model, builder);
        ModClusterDefinition.HTTP2_MAX_CONCURRENT_STREAMS.resolveOption(context, model, builder);
        ModClusterDefinition.HTTP2_MAX_FRAME_SIZE.resolveOption(context, model, builder);
        ModClusterDefinition.HTTP2_MAX_HEADER_LIST_SIZE.resolveOption(context, model, builder);
        this.clientOptions = builder.getMap();

        Resource resource = context.readResource(PathAddress.EMPTY_ADDRESS);
        Set<Resource.ResourceEntry> children = resource.getChildren(AffinityResourceDefinition.WILDCARD_PATH.getKey());
        if (children.size() != 1) {
            throw new IllegalStateException();
        }
        Resource.ResourceEntry entry = children.iterator().next();
        this.routeParsingStrategy = ROUTE_PARSING_STRATEGIES.get(entry.getPathElement());
        this.routeDelimiter = (this.routeParsingStrategy == RouteParsingStrategy.RANKED) ? RankedAffinityResourceDefinition.Attribute.DELIMITER.resolveModelAttribute(context, entry.getModel()).asString() : null;

        String managementBinding = ModClusterDefinition.MANAGEMENT_SOCKET_BINDING.resolveModelAttribute(context, model).asString();
        this.managementBinding = new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, managementBinding));

        String advertiseBinding = ModClusterDefinition.ADVERTISE_SOCKET_BINDING.resolveModelAttribute(context, model).asStringOrNull();
        this.advertiseBinding = (advertiseBinding != null) ? new ServiceSupplierDependency<>(CommonUnaryRequirement.SOCKET_BINDING.getServiceName(context, advertiseBinding)) : null;

        String worker = ModClusterDefinition.WORKER.resolveModelAttribute(context, model).asString();
        this.worker = new ServiceSupplierDependency<>(context.getCapabilityServiceName(IOServices.IO_WORKER_CAPABILITY_NAME, XnioWorker.class, worker));

        this.healthCheckInterval = ModClusterDefinition.HEALTH_CHECK_INTERVAL.resolveModelAttribute(context, model).asInt();
        this.maxRequestTime = ModClusterDefinition.MAX_REQUEST_TIME.resolveModelAttribute(context, model).asInt();
        this.brokenNodeTimeout = ModClusterDefinition.BROKEN_NODE_TIMEOUT.resolveModelAttribute(context, model).asLong();
        this.advertiseFrequency = ModClusterDefinition.ADVERTISE_FREQUENCY.resolveModelAttribute(context, model).asInt();
        this.advertisePath = ModClusterDefinition.ADVERTISE_PATH.resolveModelAttribute(context, model).asString();
        this.advertiseProtocol = ModClusterDefinition.ADVERTISE_PROTOCOL.resolveModelAttribute(context, model).asString();
        this.securityKey = ModClusterDefinition.SECURITY_KEY.resolveModelAttribute(context, model).asStringOrNull();
        this.maxConnections = ModClusterDefinition.CONNECTIONS_PER_THREAD.resolveModelAttribute(context, model).asInt();
        this.cachedConnections = ModClusterDefinition.CACHED_CONNECTIONS_PER_THREAD.resolveModelAttribute(context, model).asInt();
        this.connectionIdleTimeout = ModClusterDefinition.CONNECTION_IDLE_TIMEOUT.resolveModelAttribute(context, model).asInt();
        this.requestQueueSize = ModClusterDefinition.REQUEST_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        this.useAlias = ModClusterDefinition.USE_ALIAS.resolveModelAttribute(context, model).asBoolean();
        this.maxRetries = ModClusterDefinition.MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        this.failoverStrategy = Enum.valueOf(FailoverStrategy.class, ModClusterDefinition.FAILOVER_STRATEGY.resolveModelAttribute(context, model).asString());
        return this;
    }

    @Override
    public ServiceBuilder<?> build(ServiceTarget target) {
        ServiceName name = this.getServiceName();
        ServiceBuilder<?> builder = target.addService(name);
        Consumer<ModCluster> service = new CompositeDependency(this.worker, this.managementBinding, this.advertiseBinding, this.sslContext).register(builder).provides(name);
        Consumer<MCMPConfig> config = builder.provides(this.getConfigServiceName());
        Consumer<Map.Entry<ModCluster, MCMPConfig>> consumer = new Consumer<>() {
            @Override
            public void accept(Map.Entry<ModCluster, MCMPConfig> entry) {
                service.accept(entry.getKey());
                config.accept(entry.getValue());
            }
        };
        return builder.setInstance(new FunctionalService<>(consumer, Function.identity(), this, this));
    }

    @Override
    public void accept(Map.Entry<ModCluster, MCMPConfig> entry) {
        entry.getKey().stop();
    }

    @Override
    public Map.Entry<ModCluster, MCMPConfig> get() {
        SSLContext sslContext = this.sslContext != null ? this.sslContext.get() : null;
        XnioWorker worker = this.worker.get();
        XnioSsl xnioSsl = (sslContext != null) ? new UndertowXnioSsl(worker.getXnio(), OptionMap.builder().set(Options.USE_DIRECT_BUFFERS, true).getMap(), sslContext) : null;

        //TODO: SSL support for the client
        ModCluster.Builder serviceBuilder = ModCluster.builder(worker, UndertowClient.getInstance(), xnioSsl)
                .setMaxRetries(this.maxRetries)
                .setClientOptions(this.clientOptions)
                .setHealthCheckInterval(this.healthCheckInterval)
                .setMaxRequestTime(this.maxRequestTime)
                .setCacheConnections(this.cachedConnections)
                .setQueueNewRequests(this.requestQueueSize > 0)
                .setRequestQueueSize(this.requestQueueSize)
                .setRemoveBrokenNodes(this.brokenNodeTimeout)
                .setTtl(this.connectionIdleTimeout)
                .setMaxConnections(this.maxConnections)
                .setUseAlias(this.useAlias)
                .setRouteParsingStrategy(this.routeParsingStrategy)
                .setRankedAffinityDelimiter(this.routeDelimiter)
        ;

        if (this.failoverStrategy == FailoverStrategy.DETERMINISTIC) {
            serviceBuilder.setDeterministicFailover(true);
        }

        ModCluster service = serviceBuilder.build();

        MCMPConfig.Builder configBuilder = MCMPConfig.builder();
        if (this.advertiseBinding != null) {
            SocketBinding advertiseBinding = this.advertiseBinding.get();
            InetAddress multicastAddress = advertiseBinding.getMulticastAddress();
            if (multicastAddress == null) {
                throw UndertowLogger.ROOT_LOGGER.advertiseSocketBindingRequiresMulticastAddress();
            }
            if (this.advertiseFrequency > 0) {
                configBuilder.enableAdvertise()
                        .setAdvertiseAddress(advertiseBinding.getSocketAddress().getAddress().getHostAddress())
                        .setAdvertiseGroup(multicastAddress.getHostAddress())
                        .setAdvertisePort(advertiseBinding.getMulticastPort())
                        .setAdvertiseFrequency(this.advertiseFrequency)
                        .setPath(this.advertisePath)
                        .setProtocol(this.advertiseProtocol)
                        .setSecurityKey(this.securityKey);
            }
        }
        SocketBinding managementBinding = this.managementBinding.get();
        configBuilder.setManagementHost(managementBinding.getSocketAddress().getHostString());
        configBuilder.setManagementPort(managementBinding.getSocketAddress().getPort());

        MCMPConfig config = configBuilder.build();

        if (this.advertiseBinding != null && this.advertiseFrequency > 0) {
            try {
                service.advertise(config);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        service.start();
        return Map.entry(service, config);
    }
}
