/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2014, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.undertow.filters;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.value.InjectedValue;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class FilterRef extends AbstractService<FilterRef> {
    private final Predicate predicate;
    private final InjectedValue<FilterService> filter = new InjectedValue<>();

    public FilterRef(Predicate predicate) {
        this.predicate = predicate;
    }

    InjectedValue<FilterService> getFilter() {
        return filter;
    }

    public HttpHandler createHttpHandler(HttpHandler next) {
        return filter.getValue().createHttpHandler(predicate, next);
    }

    @Override
    public FilterRef getValue() throws IllegalStateException {
        return this;
    }
}
