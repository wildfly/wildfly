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
public class FilePassivationStoreAdd extends PassivationStoreAdd {

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws IllegalArgumentException, OperationFailedException {
        int initialMaxSize = FilePassivationStoreResourceDefinition.MAX_SIZE.resolveModelAttribute(context, model).asInt();
        String containerName = PassivationStoreResourceDefinition.CACHE_CONTAINER.getDefaultValue().asString();
        // despite being called file passivation store, this sets up a cache factory based on the default cache container and cache name "passivation"
        this.install(context, operation, initialMaxSize, containerName, "passivation");
    }
}
