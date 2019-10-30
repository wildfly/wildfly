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

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.undertow.FilterLocation;
import org.wildfly.extension.undertow.UndertowFilter;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class FilterRef implements Service<FilterRef>, UndertowFilter {
    private final Predicate predicate;
    private final int priority;
    private final InjectedValue<FilterService> filter = new InjectedValue<>();
    private final InjectedValue<FilterLocation> location = new InjectedValue<>();

    public FilterRef(Predicate predicate, int priority) {
        this.predicate = predicate;
        this.priority = priority;
    }

    @Override
    public void start(StartContext context) throws StartException {
        location.getValue().addFilter(this);
    }

    @Override
    public void stop(StopContext context) {
        location.getValue().removeFilter(this);
    }

    InjectedValue<FilterService> getFilter() {
        return filter;
    }

    InjectedValue<FilterLocation> getLocation() {
        return location;
    }

    public HttpHandler createHttpHandler(HttpHandler next) {
        return filter.getValue().createHttpHandler(predicate, next);
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public FilterRef getValue() throws IllegalStateException {
        return this;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return filter.getValue().createHttpHandler(predicate, handler);
    }
}
