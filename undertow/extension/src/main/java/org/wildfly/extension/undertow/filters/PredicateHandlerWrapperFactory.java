/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.undertow.filters;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * Factory for creating a {@link PredicateHandlerWrapper}.
 * @author Paul Ferraro
 */
public interface PredicateHandlerWrapperFactory {

    PredicateHandlerWrapper createHandlerWrapper(OperationContext context, ModelNode model) throws OperationFailedException;
}
