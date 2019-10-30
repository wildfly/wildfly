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
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.wildfly.extension.undertow.Handler;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public class FilterService implements Service<FilterService> {
    private final Handler handler;
    private final ModelNode model;

    FilterService(Handler handler, ModelNode model) {
        this.handler = handler;
        this.model = model;
    }

    @Override
    public void start(StartContext context) throws StartException {

    }

    @Override
    public void stop(StopContext context) {

    }

    public HttpHandler createHttpHandler(final Predicate predicate, HttpHandler next) {
        return handler.createHttpHandler(predicate, model, next);
    }

    @Override
    public FilterService getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }
}
