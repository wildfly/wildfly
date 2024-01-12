/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import io.undertow.predicate.Predicate;
import io.undertow.predicate.Predicates;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.PathElement;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2014 Red Hat Inc.
 */
public class GzipFilterDefinition extends SimpleFilterDefinition {
    public static final PathElement PATH_ELEMENT = PathElement.pathElement("gzip");

    GzipFilterDefinition() {
        super(PATH_ELEMENT, GzipFilterDefinition::createHandlerWrapper);
    }

    static PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) {
        return new PredicateHandlerWrapper() {
            @Override
            public HttpHandler wrap(Predicate predicate, HttpHandler next) {
                ContentEncodingRepository repository = new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(), 50, predicate != null ? predicate : Predicates.truePredicate());
                return new EncodingHandler(next, repository);
            }
        };
    }
}