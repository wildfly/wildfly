/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow;

import java.util.Collection;

import io.undertow.predicate.Predicate;
import io.undertow.server.HttpHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2013 Red Hat Inc.
 */
public interface Handler {
    Collection<AttributeDefinition> getAttributes();

    Class<? extends HttpHandler> getHandlerClass();

    HttpHandler createHttpHandler(final Predicate predicate, final ModelNode model, final HttpHandler next);

}
