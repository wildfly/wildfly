/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import io.undertow.server.HandlerWrapper;
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

    static HandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) {
        ContentEncodingRepository repository = new ContentEncodingRepository().addEncodingHandler("gzip", new GzipEncodingProvider(), 50);
        return next -> new EncodingHandler(next, repository);
    }
}