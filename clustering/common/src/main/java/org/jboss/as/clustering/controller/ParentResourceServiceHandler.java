/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller;

import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * A {@link SimpleResourceServiceHandler} that uses a recursively read model, to include child resources.
 * @author Paul Ferraro
 */
public class ParentResourceServiceHandler extends SimpleResourceServiceHandler {

    public ParentResourceServiceHandler(ResourceServiceConfiguratorFactory factory) {
        super(factory);
    }

    @Override
    public void installServices(OperationContext context, ModelNode model) throws OperationFailedException {
        super.installServices(context, Resource.Tools.readModel(context.readResource(PathAddress.EMPTY_ADDRESS)));
    }
}
