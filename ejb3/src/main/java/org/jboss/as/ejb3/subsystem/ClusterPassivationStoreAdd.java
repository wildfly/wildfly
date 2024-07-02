/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.dmr.ModelNode;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class ClusterPassivationStoreAdd extends PassivationStoreAdd {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws IllegalArgumentException, OperationFailedException {
        int initialMaxSize = ClusterPassivationStoreResourceDefinition.MAX_SIZE.resolveModelAttribute(context, model).asInt();
        String containerName = ClusterPassivationStoreResourceDefinition.CACHE_CONTAINER.resolveModelAttribute(context, model).asString();
        ModelNode beanCacheNode = ClusterPassivationStoreResourceDefinition.BEAN_CACHE.resolveModelAttribute(context, model);
        String cacheName = beanCacheNode.isDefined() ? beanCacheNode.asString() : null;
        this.install(context, operation, initialMaxSize, containerName, cacheName);
    }
}
