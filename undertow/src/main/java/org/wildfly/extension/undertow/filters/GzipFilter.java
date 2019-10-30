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
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class GzipFilter extends Filter {


    public static final GzipFilter INSTANCE = new GzipFilter();

    private GzipFilter() {
        super("gzip");
    }

    @Override
    public HttpHandler createHttpHandler(final Predicate predicate, ModelNode model, HttpHandler next) {
        EncodingHandler encodingHandler = new EncodingHandler(new ContentEncodingRepository()
                .addEncodingHandler("gzip", new GzipEncodingProvider(), 50, predicate != null ? predicate : Predicates.truePredicate()));
        encodingHandler.setNext(next);
        return encodingHandler;
    }

    @Override
    protected Class[] getConstructorSignature() {
        throw new IllegalStateException(); //should not be used, as the handler is constructed above
    }
}