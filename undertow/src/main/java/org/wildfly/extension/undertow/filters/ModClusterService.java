/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import static org.wildfly.extension.undertow.Capabilities.REF_SSL_CONTEXT;
import io.undertow.Handlers;
import io.undertow.UndertowOptions;
import io.undertow.client.UndertowClient;
import io.undertow.predicate.PredicateParser;
import io.undertow.protocols.ssl.UndertowXnioSsl;
import org.jboss.as.controller.CapabilityServiceBuilder;
import org.jboss.as.controller.CapabilityServiceTarget;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.domain.management.SecurityRealm;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.undertow.Capabilities;
import org.wildfly.extension.undertow.UndertowService;
import org.wildfly.extension.undertow.logging.UndertowLogger;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.XnioWorker;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.proxy.mod_cluster.MCMPConfig;
import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;
import org.xnio.ssl.XnioSsl;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetAddress;

/**
 * filter service for the mod cluster frontend. This requires various injections, and as a result can't use the
 * standard filter service
 *
 * @author Stuart Douglas
 */
public class ModClusterService extends FilterService {

    private final InjectedValue<XnioWorker> workerInjectedValue = new InjectedValue<>();
    private final InjectedValue<SocketBinding> managementSocketBinding = new InjectedValue<>();
    private final InjectedValue<SocketBinding> advertiseSocketBinding = new InjectedValue<>();
    private final InjectedValue<SecurityRealm> securityRealm = new InjectedValue<>();
    private final InjectedValue<SSLContext> sslContext = new InjectedValue<>();
    private final long healthCheckInterval;
    private final int maxRequestTime;
    private final long removeBrokenNodes;
    private final int advertiseFrequency;
    private final String advertisePath;
    private final String advertiseProtocol;
    private final String securityKey;
    private final Predicate managementAccessPredicate;
    private final int connectionsPerThread;
    private final int cachedConnections;
    private final int connectionIdleTimeout;
    private final int requestQueueSize;
    private final boolean useAlias;
    private final int maxRetries;
    private final FailoverStrategy failoverStrategy;

    private ModCluster modCluster;
    private MCMPConfig config;
    private final OptionMap clientOptions;

    ModClusterService(ModelNode model,
                      long healthCheckInterval,
                      int maxRequestTime,
                      long removeBrokenNodes,
                      int advertiseFrequency,
                      String advertisePath,
                      String advertiseProtocol,
                      String securityKey,
                      Predicate managementAccessPredicate,
                      int connectionsPerThread,
                      int cachedConnections,
                      int connectionIdleTimeout,
                      int requestQueueSize,
                      boolean useAlias,
                      int maxRetries,
                      FailoverStrategy failoverStrategy,
                      OptionMap clientOptions) {
        super(ModClusterDefinition.INSTANCE, model);
        this.healthCheckInterval = healthCheckInterval;
        this.maxRequestTime = maxRequestTime;
        this.removeBrokenNodes = removeBrokenNodes;
        this.advertiseFrequency = advertiseFrequency;
        this.advertisePath = advertisePath;
        this.advertiseProtocol = advertiseProtocol;
        this.securityKey = securityKey;
        this.managementAccessPredicate = managementAccessPredicate;
        this.connectionsPerThread = connectionsPerThread;
        this.cachedConnections = cachedConnections;
        this.connectionIdleTimeout = connectionIdleTimeout;
        this.requestQueueSize = requestQueueSize;
        this.useAlias = useAlias;
        this.maxRetries = maxRetries;
        this.failoverStrategy = failoverStrategy;
        this.clientOptions = clientOptions;
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);

        SSLContext sslContext = this.sslContext.getOptionalValue();
        if (sslContext == null) {
            SecurityRealm realm = securityRealm.getOptionalValue();
            if (realm != null) {
                sslContext = realm.getSSLContext();
            }
        }

        //TODO: SSL support for the client
        final ModCluster.Builder modClusterBuilder;
        final XnioWorker worker = workerInjectedValue.getValue();
        if(sslContext == null) {
            modClusterBuilder = ModCluster.builder(worker);
        } else {
            OptionMap.Builder builder = OptionMap.builder();
            builder.set(Options.USE_DIRECT_BUFFERS, true);
            OptionMap combined = builder.getMap();

            XnioSsl xnioSsl = new UndertowXnioSsl(worker.getXnio(), combined, sslContext);
            modClusterBuilder = ModCluster.builder(worker, UndertowClient.getInstance(), xnioSsl);
        }
        modClusterBuilder
                .setMaxRetries(maxRetries)
                .setClientOptions(clientOptions)
                .setHealthCheckInterval(healthCheckInterval)
                .setMaxRequestTime(maxRequestTime)
                .setCacheConnections(cachedConnections)
                .setQueueNewRequests(requestQueueSize > 0)
                .setRequestQueueSize(requestQueueSize)
                .setRemoveBrokenNodes(removeBrokenNodes)
                .setTtl(connectionIdleTimeout)
                .setMaxConnections(connectionsPerThread)
                .setUseAlias(useAlias);

        if (FailoverStrategy.DETERMINISTIC.equals(failoverStrategy)) {
            modClusterBuilder.setDeterministicFailover(true);
        }

        modCluster = modClusterBuilder.build();

        MCMPConfig.Builder builder = MCMPConfig.builder();
        final SocketBinding advertiseBinding = advertiseSocketBinding.getOptionalValue();
        if (advertiseBinding != null) {
            InetAddress multicastAddress = advertiseBinding.getMulticastAddress();
            if (multicastAddress == null) {
                throw UndertowLogger.ROOT_LOGGER.advertiseSocketBindingRequiresMulticastAddress();
            }
            if (advertiseFrequency > 0) {
                builder.enableAdvertise()
                        .setAdvertiseAddress(advertiseBinding.getSocketAddress().getAddress().getHostAddress())
                        .setAdvertiseGroup(multicastAddress.getHostAddress())
                        .setAdvertisePort(advertiseBinding.getMulticastPort())
                        .setAdvertiseFrequency(advertiseFrequency)
                        .setPath(advertisePath)
                        .setProtocol(advertiseProtocol)
                        .setSecurityKey(securityKey);
            }
        }
        builder.setManagementHost(managementSocketBinding.getValue().getSocketAddress().getHostString());
        builder.setManagementPort(managementSocketBinding.getValue().getSocketAddress().getPort());

        config = builder.build();


        if (advertiseBinding != null && advertiseFrequency > 0) {
            try {
                modCluster.advertise(config);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        modCluster.start();
    }

    @Override
    public synchronized void stop(StopContext context) {
        super.stop(context);
        modCluster.stop();
        modCluster = null;
        config = null;
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, final HttpHandler next) {
        //this is a bit of a hack at the moment. Basically we only want to create a single mod_cluster instance
        //not matter how many filter refs use it, also mod_cluster at this point has no way
        //to specify the next handler. To get around this we invoke the mod_proxy handler
        //and then if it has not dispatched or handled the request then we know that we can
        //just pass it on to the next handler
        final HttpHandler proxyHandler = modCluster.createProxyHandler(next);
        final HttpHandler realNext = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                proxyHandler.handleRequest(exchange);
                if(!exchange.isDispatched() && !exchange.isComplete()) {
                    exchange.setStatusCode(200);
                    next.handleRequest(exchange);
                }
            }
        };
        final HttpHandler mcmp = managementAccessPredicate != null  ? Handlers.predicate(managementAccessPredicate, config.create(modCluster, realNext), next)  :  config.create(modCluster, realNext);

        UndertowLogger.ROOT_LOGGER.debug("HttpHandler for mod_cluster MCMP created.");
        if (predicate != null) {
            return new PredicateHandler(predicate, mcmp, next);
        } else {
            return mcmp;
        }
    }

    static void install(String name, CapabilityServiceTarget serviceTarget, ModelNode model, OperationContext operationContext) throws OperationFailedException {
        String securityKey = null;
        ModelNode securityKeyNode = ModClusterDefinition.SECURITY_KEY.resolveModelAttribute(operationContext, model);
        if(securityKeyNode.isDefined()) {
            securityKey = securityKeyNode.asString();
        }

        String managementAccessPredicateString = null;
        ModelNode managementAccessPredicateNode = ModClusterDefinition.MANAGEMENT_ACCESS_PREDICATE.resolveModelAttribute(operationContext, model);
        if(managementAccessPredicateNode.isDefined()) {
            managementAccessPredicateString = managementAccessPredicateNode.asString();
        }
        Predicate managementAccessPredicate = null;
        if(managementAccessPredicateString != null) {
            managementAccessPredicate = PredicateParser.parse(managementAccessPredicateString, ModClusterService.class.getClassLoader());
        }
        final ModelNode sslContext = ModClusterDefinition.SSL_CONTEXT.resolveModelAttribute(operationContext, model);
        final ModelNode securityRealm = ModClusterDefinition.SECURITY_REALM.resolveModelAttribute(operationContext, model);

        final ModelNode packetSizeNode = ModClusterDefinition.MAX_AJP_PACKET_SIZE.resolveModelAttribute(operationContext, model);
        OptionMap.Builder builder = OptionMap.builder();
        if(packetSizeNode.isDefined()) {
            builder.set(UndertowOptions.MAX_AJP_PACKET_SIZE, packetSizeNode.asInt());
        }
        builder.set(UndertowOptions.ENABLE_HTTP2, ModClusterDefinition.ENABLE_HTTP2.resolveModelAttribute(operationContext, model).asBoolean());
        ModClusterDefinition.HTTP2_ENABLE_PUSH.resolveOption(operationContext, model, builder);
        ModClusterDefinition.HTTP2_HEADER_TABLE_SIZE.resolveOption(operationContext, model, builder);
        ModClusterDefinition.HTTP2_INITIAL_WINDOW_SIZE.resolveOption(operationContext, model, builder);
        ModClusterDefinition.HTTP2_MAX_CONCURRENT_STREAMS.resolveOption(operationContext, model, builder);
        ModClusterDefinition.HTTP2_MAX_FRAME_SIZE.resolveOption(operationContext, model, builder);
        ModClusterDefinition.HTTP2_MAX_HEADER_LIST_SIZE.resolveOption(operationContext, model, builder);

        ModClusterService service = new ModClusterService(model,
                ModClusterDefinition.HEALTH_CHECK_INTERVAL.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.MAX_REQUEST_TIME.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.BROKEN_NODE_TIMEOUT.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.ADVERTISE_FREQUENCY.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.ADVERTISE_PATH.resolveModelAttribute(operationContext, model).asString(),
                ModClusterDefinition.ADVERTISE_PROTOCOL.resolveModelAttribute(operationContext, model).asString(),
                securityKey, managementAccessPredicate,
                ModClusterDefinition.CONNECTIONS_PER_THREAD.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.CACHED_CONNECTIONS_PER_THREAD.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.CONNECTION_IDLE_TIMEOUT.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.REQUEST_QUEUE_SIZE.resolveModelAttribute(operationContext, model).asInt(),
                ModClusterDefinition.USE_ALIAS.resolveModelAttribute(operationContext, model).asBoolean(),
                ModClusterDefinition.MAX_RETRIES.resolveModelAttribute(operationContext, model).asInt(),
                Enum.valueOf(FailoverStrategy.class, ModClusterDefinition.FAILOVER_STRATEGY.resolveModelAttribute(operationContext, model).asString()),
                builder.getMap());

        final String mgmtSocketBindingRef = ModClusterDefinition.MANAGEMENT_SOCKET_BINDING.resolveModelAttribute(operationContext, model).asString();
        final ModelNode advertiseSocketBindingRef = ModClusterDefinition.ADVERTISE_SOCKET_BINDING.resolveModelAttribute(operationContext, model);
        final String workerRef = ModClusterDefinition.WORKER.resolveModelAttribute(operationContext, model).asString();
        CapabilityServiceBuilder serviceBuilder = serviceTarget.addCapability(ModClusterDefinition.MOD_CLUSTER_FILTER_CAPABILITY, service);
        serviceBuilder.addCapabilityRequirement(Capabilities.REF_SOCKET_BINDING, SocketBinding.class, service.managementSocketBinding, mgmtSocketBindingRef);
        if (advertiseSocketBindingRef.isDefined()) {
            serviceBuilder.addCapabilityRequirement(Capabilities.REF_SOCKET_BINDING, SocketBinding.class, service.advertiseSocketBinding, advertiseSocketBindingRef.asString());
        }
        serviceBuilder.addCapabilityRequirement(IOServices.IO_WORKER_CAPABILITY_NAME,XnioWorker.class, service.workerInjectedValue, workerRef);

        if (sslContext.isDefined()) {
            serviceBuilder.addCapabilityRequirement(REF_SSL_CONTEXT, SSLContext.class, service.sslContext, sslContext.asString());
        }
        if(securityRealm.isDefined()) {
            SecurityRealm.ServiceUtil.addDependency(serviceBuilder, service.securityRealm, securityRealm.asString());
        }
        serviceBuilder.addAliases(UndertowService.FILTER.append(name));
        serviceBuilder.install();
    }

    public ModCluster getModCluster() {
        return modCluster;
    }
}
