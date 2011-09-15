/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jboss.as.clustering.subsystem.AbstractExtensionTest;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.registry.Resource;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ModelDescriptionValidator.ValidationConfiguration;
import org.jboss.dmr.ModelNode;

public class JGroupsExtensionTest extends AbstractExtensionTest {

    public JGroupsExtensionTest() {
        this(new JGroupsExtension());
    }

    private JGroupsExtensionTest(JGroupsExtension extension) {
        super(extension, extension, "subsystem-jgroups.xml", Namespace.CURRENT.getUri());
    }

    @Override
    protected ModelNode populate(List<ModelNode> operations) throws OperationFailedException {
        OperationContext context = mock(OperationContext.class);
        Resource resource = mock(Resource.class);
        ModelNode model = new ModelNode();

        when(context.createResource(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);
        when(resource.getModel()).thenReturn(model);

        new JGroupsSubsystemAdd().execute(context, operations.get(0));

        model.get(ModelKeys.STACK).setEmptyList();

        for (ModelNode operation: operations.subList(1, operations.size())) {
            String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
            ModelNode stack = new ModelNode();

            when(context.createResource(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);
            when(resource.getModel()).thenReturn(stack);

            new ProtocolStackAdd().execute(context, operation);

            model.get(ModelKeys.STACK).add(name, stack);
        }
        return model;
    }

    protected AdditionalInitialization createAdditionalInitialization() {
        return new AdditionalInitialization(){
            @Override
            protected OperationContext.Type getType() {
                return OperationContext.Type.MANAGEMENT;
            }


            @Override
            protected ValidationConfiguration getModelValidationConfiguration() {
                //TODO fix validation https://issues.jboss.org/browse/AS7-1786
                return null;
            }
        };
    }

}
