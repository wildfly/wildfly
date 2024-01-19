/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import java.util.function.Consumer;
import java.util.function.Supplier;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.FilterLocation;
import org.wildfly.extension.undertow.UndertowFilter;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class FilterService implements Service<UndertowFilter>, UndertowFilter {
    private final Consumer<UndertowFilter> serviceConsumer;
    private final Supplier<PredicateHandlerWrapper> wrapper;
    private final Supplier<FilterLocation> location;
    private final Predicate predicate;
    private final int priority;

    FilterService(final Consumer<UndertowFilter> serviceConsumer, final Supplier<PredicateHandlerWrapper> wrapper, final Supplier<FilterLocation> location, final Predicate predicate, final int priority) {
        this.serviceConsumer = serviceConsumer;
        this.wrapper = wrapper;
        this.location = location;
        this.predicate = predicate;
        this.priority = priority;
    }

    @Override
    public void start(final StartContext context) throws StartException {
        location.get().addFilter(this);
        serviceConsumer.accept(this);
    }

    @Override
    public void stop(final StopContext context) {
        serviceConsumer.accept(null);
        location.get().removeFilter(this);
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public UndertowFilter getValue() {
        return this;
    }

    @Override
    public HttpHandler wrap(HttpHandler next) {
        return this.wrapper.get().wrap(this.predicate, next);
    }
}
