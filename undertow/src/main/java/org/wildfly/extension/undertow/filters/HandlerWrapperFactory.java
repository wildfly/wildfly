/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import io.undertow.server.HandlerWrapper;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Factory for creating a {@link HandlerWrapper}.
 * @author Paul Ferraro
 */
public interface HandlerWrapperFactory {
    HandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException;
}
