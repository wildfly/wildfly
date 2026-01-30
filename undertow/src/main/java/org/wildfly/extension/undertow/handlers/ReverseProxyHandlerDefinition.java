/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.Headers;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.version.Stability;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

import java.util.Collection;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class ReverseProxyHandlerDefinition extends HandlerDefinition {
    public static final PathElement PATH_ELEMENT =PathElement.pathElement(Constants.REVERSE_PROXY);

    public static final AttributeDefinition PROBLEM_SERVER_RETRY = new SimpleAttributeDefinitionBuilder(Constants.PROBLEM_SERVER_RETRY, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(30))
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition SESSION_COOKIE_NAMES = new SimpleAttributeDefinitionBuilder(Constants.SESSION_COOKIE_NAMES, ModelType.STRING)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("JSESSIONID"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();
    public static final AttributeDefinition CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CONNECTIONS_PER_THREAD, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(40))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition MAX_REQUEST_TIME = new SimpleAttributeDefinitionBuilder(Constants.MAX_REQUEST_TIME, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(-1))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition REQUEST_QUEUE_SIZE = new SimpleAttributeDefinitionBuilder(Constants.REQUEST_QUEUE_SIZE, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();


    public static final AttributeDefinition CACHED_CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CACHED_CONNECTIONS_PER_THREAD, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(5))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition CONNECTION_IDLE_TIMEOUT = new SimpleAttributeDefinitionBuilder(Constants.CONNECTION_IDLE_TIMEOUT, ModelType.INT)
            .setRequired(false)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(60000))
            .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final AttributeDefinition MAX_RETRIES = new SimpleAttributeDefinitionBuilder(Constants.MAX_RETRIES, ModelType.INT)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(1L))
            .build();

    public static final AttributeDefinition REUSE_X_FORWARDED_HEADER = new SimpleAttributeDefinitionBuilder(Constants.REUSE_X_FORWARDED_HEADER, ModelType.BOOLEAN)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    public static final AttributeDefinition REWRITE_HOST_HEADER = new SimpleAttributeDefinitionBuilder(Constants.REWRITE_HOST_HEADER, ModelType.BOOLEAN)
            .setRequired(false)
            .setRestartAllServices()
            .setAllowExpression(true)
            .setDefaultValue(ModelNode.FALSE)
            .build();

    public static final Collection<AttributeDefinition> ATTRIBUTES = List.of(CONNECTIONS_PER_THREAD, SESSION_COOKIE_NAMES, PROBLEM_SERVER_RETRY, REQUEST_QUEUE_SIZE, MAX_REQUEST_TIME, CACHED_CONNECTIONS_PER_THREAD, CONNECTION_IDLE_TIMEOUT, MAX_RETRIES,REUSE_X_FORWARDED_HEADER, REWRITE_HOST_HEADER);

    ReverseProxyHandlerDefinition() {
        super(PATH_ELEMENT, ReverseProxyHandlerDefinition::createHandler);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return ATTRIBUTES;
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return List.of(new ReverseProxyHandlerHostDefinition());
    }

    static HttpHandler createHandler(final OperationContext context, ModelNode model) throws OperationFailedException {

        String sessionCookieNames = SESSION_COOKIE_NAMES.resolveModelAttribute(context, model).asString();
        int connectionsPerThread = CONNECTIONS_PER_THREAD.resolveModelAttribute(context, model).asInt();
        int problemServerRetry = PROBLEM_SERVER_RETRY.resolveModelAttribute(context, model).asInt();
        int maxTime = MAX_REQUEST_TIME.resolveModelAttribute(context, model).asInt();
        int requestQueueSize = REQUEST_QUEUE_SIZE.resolveModelAttribute(context, model).asInt();
        int cachedConnectionsPerThread = CACHED_CONNECTIONS_PER_THREAD.resolveModelAttribute(context, model).asInt();
        int connectionIdleTimeout = CONNECTION_IDLE_TIMEOUT.resolveModelAttribute(context, model).asInt();
        int maxRetries = MAX_RETRIES.resolveModelAttribute(context, model).asInt();
        final boolean reuseXForwardedHeader = REUSE_X_FORWARDED_HEADER.resolveModelAttribute(context, model).asBoolean();
        final boolean rewriteHostHeader = REWRITE_HOST_HEADER.resolveModelAttribute(context, model).asBoolean();


        final LoadBalancingProxyClient lb = new LoadBalancingProxyClient(exchange -> {
            //we always create a new connection for upgrade requests
            return exchange.getRequestHeaders().contains(Headers.UPGRADE);
        })
                .setConnectionsPerThread(connectionsPerThread)
                .setMaxQueueSize(requestQueueSize)
                .setSoftMaxConnectionsPerThread(cachedConnectionsPerThread)
                .setTtl(connectionIdleTimeout)
                .setProblemServerRetry(problemServerRetry);
        String[] sessionIds = sessionCookieNames.split(",");
        for (String id : sessionIds) {
            lb.addSessionCookieName(id);
        }

        return ProxyHandler.builder()
                .setProxyClient(lb)
                .setMaxRequestTime(maxTime)
                .setNext(ResponseCodeHandler.HANDLE_404)
                .setRewriteHostHeader(false)
                .setReuseXForwarded(false)
                .setMaxConnectionRetries(maxRetries)
                .setRewriteHostHeader(rewriteHostHeader)
                .setReuseXForwarded(reuseXForwardedHeader)
                .build();
    }
}
