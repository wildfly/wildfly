/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.webservices.deployers;

import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpServerExchange;

import org.jboss.metadata.web.jboss.JBossServletMetaData;
import org.jboss.metadata.web.jboss.JBossWebMetaData;
import org.jboss.metadata.web.spec.ServletMappingMetaData;
import org.jboss.wsf.spi.deployment.WSFServlet;

public class AllowWSRequestPredicate implements Predicate, UnaryOperator<Set<String>> {

    private final AtomicReference<Set<String>> paths = new AtomicReference<>();
    private final Supplier<Set<String>> factory;

    public AllowWSRequestPredicate(JBossWebMetaData metaData) {
        // Process metadata lazily, as metadata may not yet be complete!
        this.factory = new Supplier<>() {
            @Override
            public Set<String> get() {
                if (metaData != null) {
                    String contextPath = metaData.getContextRoot();
                    Set<String> servletNames = new TreeSet<>();
                    for (JBossServletMetaData servlet : metaData.getServlets()) {
                        if (WSFServlet.class.getName().equals(servlet.getServletClass())) {
                            servletNames.add(servlet.getName());
                        }
                    }
                    if (!servletNames.isEmpty()) {
                        Set<String> paths = new TreeSet<>();
                        for (ServletMappingMetaData mapping : metaData.getServletMappings()) {
                            if (servletNames.contains(mapping.getServletName())) {
                                for (String path : mapping.getUrlPatterns()) {
                                    paths.add((contextPath != null) ? contextPath + path : path);
                                }
                            }
                        }
                        if (!paths.isEmpty()) {
                            return paths;
                        }
                    }
                }
                return Set.of();
            }
        };
    }

    @Override
    public boolean resolve(HttpServerExchange exchange) {
        // Match exact path only
        return this.getPaths().contains(exchange.getRequestPath());
    }

    private Set<String> getPaths() {
        // Compute-if-absent
        Set<String> paths = this.paths.get();
        return (paths != null) ? paths : this.paths.updateAndGet(this);
    }

    @Override
    public Set<String> apply(Set<String> paths) {
        return (paths != null) ? paths : this.factory.get();
    }
}
