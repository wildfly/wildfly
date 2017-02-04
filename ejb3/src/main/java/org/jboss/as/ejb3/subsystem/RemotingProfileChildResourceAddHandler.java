/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.ejb3.subsystem;

import java.util.Collection;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;

/**
 *
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */

public class RemotingProfileChildResourceAddHandler extends RemotingProfileChildResourceHandlerBase {

    private final Collection<? extends AttributeDefinition>attributes;

    protected RemotingProfileChildResourceAddHandler(Collection<? extends AttributeDefinition> attributes) {
        this.attributes = attributes;
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
