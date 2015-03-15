package org.wildfly.extension.undertow.filters;

import io.undertow.Handlers;
import io.undertow.predicate.PredicateParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.network.SocketBinding;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.io.IOServices;
import org.wildfly.extension.undertow.UndertowService;
import org.xnio.XnioWorker;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.proxy.mod_cluster.MCMPConfig;
import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;

import java.io.IOException;

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

    private ModCluster modCluster;
    private MCMPConfig config;

    ModClusterService(ModelNode model, long healthCheckInterval, int maxRequestTime, long removeBrokenNodes, int advertiseFrequency, String advertisePath, String advertiseProtocol, String securityKey, Predicate managementAccessPredicate, int connectionsPerThread, int cachedConnections, int connectionIdleTimeout, int requestQueueSize) {
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
    }

    @Override
    public synchronized void start(StartContext context) throws StartException {
        super.start(context);

        //TODO: SSL support for the client
        //TODO: wire up idle timeout when new version of undertow arrives
        modCluster = ModCluster.builder(workerInjectedValue.getValue())
                .setHealthCheckInterval(healthCheckInterval)
                .setMaxRequestTime(maxRequestTime)
                .setCacheConnections(cachedConnections)
                .setQueueNewRequests(requestQueueSize > 0)
                .setRequestQueueSize(requestQueueSize)
                .setRemoveBrokenNodes(removeBrokenNodes)
                .build();

        MCMPConfig.Builder builder = MCMPConfig.builder();
        builder.enableAdvertise()
                .setAdvertiseAddress(advertiseSocketBinding.getValue().getSocketAddress().getAddress().getHostAddress())
                .setAdvertiseGroup(advertiseSocketBinding.getValue().getMulticastAddress().getHostAddress())
                .setAdvertisePort(advertiseSocketBinding.getValue().getPort())
                .setAdvertiseFrequency(advertiseFrequency)
                .setPath(advertisePath)
                .setProtocol(advertiseProtocol)
                .setSecurityKey(securityKey);
        builder.setManagementHost(managementSocketBinding.getValue().getSocketAddress().getHostName());
        builder.setManagementPort(managementSocketBinding.getValue().getSocketAddress().getPort());
        config = builder.build();

        try {
            modCluster.advertise(config);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        final HttpHandler mcmp = managementAccessPredicate != null  ? Handlers.predicate(managementAccessPredicate, config.create(modCluster, next), next)  :  config.create(modCluster, next);
        final HttpHandler proxyHandler = modCluster.getProxyHandler();
        HttpHandler theHandler = new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                proxyHandler.handleRequest(exchange);
                if(!exchange.isDispatched() && !exchange.isComplete()) {
                    exchange.setResponseCode(200);
                    mcmp.handleRequest(exchange);
                }
            }
        };
        if(predicate != null) {
            return new PredicateHandler(predicate, theHandler, next);
        } else {
            return theHandler;
        }
    }

    static ServiceController<FilterService> install(String name, ServiceTarget serviceTarget, ModelNode model, OperationContext operationContext) throws OperationFailedException {
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
                ModClusterDefinition.REQUEST_QUEUE_SIZE.resolveModelAttribute(operationContext, model).asInt());
        ServiceBuilder<FilterService> builder = serviceTarget.addService(UndertowService.FILTER.append(name), service);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(ModClusterDefinition.MANAGEMENT_SOCKET_BINDING.resolveModelAttribute(operationContext, model).asString()), SocketBinding.class, service.managementSocketBinding);
        builder.addDependency(SocketBinding.JBOSS_BINDING_NAME.append(ModClusterDefinition.ADVERTISE_SOCKET_BINDING.resolveModelAttribute(operationContext, model).asString()), SocketBinding.class, service.advertiseSocketBinding);
        builder.addDependency(IOServices.WORKER.append(ModClusterDefinition.WORKER.resolveModelAttribute(operationContext, model).asString()), XnioWorker.class, service.workerInjectedValue);
        return builder.install();
    }
}
