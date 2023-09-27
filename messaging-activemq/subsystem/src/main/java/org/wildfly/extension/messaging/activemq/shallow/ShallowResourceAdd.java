/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.wildfly.extension.messaging.activemq.shallow;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 * Abstract class to Add a ShallowResource.
 * @author Emmanuel Hugonnet (c) 2019 Red Hat, Inc.
 */
public abstract class ShallowResourceAdd extends AbstractAddStepHandler {


    protected ShallowResourceAdd(AttributeDefinition... attributes) {
        super(attributes);
    }

    @Override
    protected void populateModel(final ModelNode operation, final ModelNode model) throws OperationFailedException {
        //do nothing
    }

    @Override
    protected Resource createResource(OperationContext context) {
        final Resource toAdd = Resource.Factory.create(true);
        context.addResource(PathAddress.EMPTY_ADDRESS, toAdd);
        return toAdd;
    }
}
