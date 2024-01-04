/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.util.Arrays;
import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationDescriptor;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class RemotingProfileChildResourceAddHandler extends RemotingProfileChildResourceHandlerBase implements OperationDescriptor {

    private final Collection<AttributeDefinition> attributes;

    protected RemotingProfileChildResourceAddHandler(AttributeDefinition... attributes) {
        this.attributes = Arrays.asList(attributes);
    }

    @Override
    public Collection<? extends AttributeDefinition> getAttributes() {
        return this.attributes;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    @Override
    protected void updateModel(final OperationContext context,final ModelNode operation) throws OperationFailedException {
        final Resource resource = context.createResource(PathAddress.EMPTY_ADDRESS);
        populateModel(operation, resource.getModel());
    }

    protected void populateModel(final ModelNode operation,final ModelNode model) throws OperationFailedException {
        for (final AttributeDefinition attr : this.attributes) {
            attr.validateAndSet(operation, model);
        }
    }
}
