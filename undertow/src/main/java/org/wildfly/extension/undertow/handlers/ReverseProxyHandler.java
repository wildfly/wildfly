/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow.handlers;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PersistentResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.extension.undertow.Constants;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Stuart Douglas
 */
public class ReverseProxyHandler extends Handler {

    public static final ReverseProxyHandler INSTANCE = new ReverseProxyHandler();

    public static final AttributeDefinition PROBLEM_SERVER_RETRY = new SimpleAttributeDefinitionBuilder(Constants.PROBLEM_SERVER_RETRY, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(30))
            .build();

    public static final AttributeDefinition SESSION_COOKIE_NAMES = new SimpleAttributeDefinitionBuilder(Constants.SESSION_COOKIE_NAMES, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode("JSESSIONID"))
            .build();

    public static final AttributeDefinition CONNECTIONS_PER_THREAD = new SimpleAttributeDefinitionBuilder(Constants.CONNECTIONS_PER_THREAD, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setDefaultValue(new ModelNode(10))
            .build();

    public static final AttributeDefinition MAX_REQUEST_TIME = new SimpleAttributeDefinitionBuilder(Constants.MAX_REQUEST_TIME, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    private ReverseProxyHandler() {
        super(Constants.REVERSE_PROXY);
    }

    @Override
    public Collection<AttributeDefinition> getAttributes() {
        return Arrays.asList(CONNECTIONS_PER_THREAD, SESSION_COOKIE_NAMES, PROBLEM_SERVER_RETRY);
    }

    @Override
    protected List<? extends PersistentResourceDefinition> getChildren() {
        return Collections.<PersistentResourceDefinition>singletonList(ReverseProxyHandlerHost.INSTANCE);
    }

    @Override
    public HttpHandler createHandler(final OperationContext context, ModelNode model) throws OperationFailedException {

        String sessionCookieNames = SESSION_COOKIE_NAMES.resolveModelAttribute(context, model).asString();
        int connectionsPerThread = CONNECTIONS_PER_THREAD.resolveModelAttribute(context, model).asInt();
        int problemServerRetry = PROBLEM_SERVER_RETRY.resolveModelAttribute(context, model).asInt();
        ModelNode maxTimeNode = MAX_REQUEST_TIME.resolveModelAttribute(context, model);
        int maxTime = -1;
        if (maxTimeNode.isDefined()) {
            maxTime = maxTimeNode.asInt();
        }
        final LoadBalancingProxyClient lb = new LoadBalancingProxyClient()
                .setConnectionsPerThread(connectionsPerThread)
                .setProblemServerRetry(problemServerRetry);
        String[] sessionIds = sessionCookieNames.split(",");
        for (String id : sessionIds) {
            lb.addSessionCookieName(id);
        }

        ProxyHandler handler = new ProxyHandler(lb, maxTime, ResponseCodeHandler.HANDLE_404);
        return handler;
    }
}
