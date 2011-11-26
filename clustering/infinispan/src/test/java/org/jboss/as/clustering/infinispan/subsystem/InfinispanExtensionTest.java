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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.jboss.as.clustering.subsystem.AbstractExtensionTest;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.as.controller.registry.Resource;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.junit.Ignore;

/**
 * @author Paul Ferraro
 */
@Ignore
public class InfinispanExtensionTest extends AbstractExtensionTest {

    public InfinispanExtensionTest() {
        this(InfinispanSubsystemParser_1_0.getInstance(), InfinispanSubsystemParser_1_0.getInstance());
    }

    private InfinispanExtensionTest(XMLElementReader<List<ModelNode>> reader, XMLElementWriter<SubsystemMarshallingContext> writer) {
        super(reader, writer, "subsystem-infinispan.xml", Namespace.CURRENT.getUri());
    }

    @Override
    protected ModelNode populate(List<ModelNode> operations) throws OperationFailedException {
        OperationContext context = mock(OperationContext.class);
        Resource resource = mock(Resource.class);
        ModelNode model = new ModelNode();

        when(context.createResource(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);
        when(resource.getModel()).thenReturn(model);

        // execute the subsystem add command
        new InfinispanSubsystemAdd().execute(context, operations.get(0));

        model.get(ModelKeys.CACHE_CONTAINER).setEmptyList();

        // TODO -fix this test
        // excute remaining commands (this has now changed with the refactoring)
        // add commands are now:
        // [0] subsystem
        // [1] cache-container minimal
        // [2] cache local
        // [3] cache-container maximal
        // [4] cache local
        // [5] cache dist
        // [6] cache invalid
        // [7] cache repl

        for (ModelNode operation: operations.subList(1, operations.size())) {
            String name = PathAddress.pathAddress(operation.get(OP_ADDR)).getLastElement().getValue();
            ModelNode container = new ModelNode();

            when(context.createResource(PathAddress.EMPTY_ADDRESS)).thenReturn(resource);
            when(resource.getModel()).thenReturn(container);

            new CacheContainerAdd().execute(context, operation);

            model.get(ModelKeys.CACHE_CONTAINER).add(name, container);
        }
        return model;
    }
}
