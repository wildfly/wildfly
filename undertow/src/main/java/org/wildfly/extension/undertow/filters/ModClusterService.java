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

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.Handlers;
import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PredicateHandler;
import io.undertow.server.handlers.proxy.mod_cluster.MCMPConfig;
import io.undertow.server.handlers.proxy.mod_cluster.ModCluster;

import org.wildfly.extension.undertow.logging.UndertowLogger;

/**
 * Filter service for the mod_cluster frontend. This requires various injections, and as a result can't use the
 * standard filter service
 *
 * @author Stuart Douglas
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author Radoslav Husar
 */
public class ModClusterService extends FilterService {

    private final Supplier<ModCluster> service;
    private final Supplier<MCMPConfig> config;
    private final Predicate managementAccessPredicate;

    ModClusterService(Consumer<FilterService> filter, Supplier<ModCluster> service, Supplier<MCMPConfig> config, Predicate managementAccessPredicate) {
        super(filter, null, null);
        this.service = service;
        this.config = config;
        this.managementAccessPredicate = managementAccessPredicate;
    }

    @Override
    public HttpHandler createHttpHandler(Predicate predicate, final HttpHandler next) {
        ModCluster modCluster = this.service.get();
        MCMPConfig config = this.config.get();
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
}
