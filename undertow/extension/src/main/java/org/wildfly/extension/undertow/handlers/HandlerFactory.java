/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.handlers;

import io.undertow.server.HttpHandler;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Factory for creating {@link HttpHandler} implementations from a resource model
 * @author Paul Ferraro
 */
public interface HandlerFactory {
    HttpHandler createHandler(final OperationContext context, ModelNode model) throws OperationFailedException;
}
