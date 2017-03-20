/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.controller.transform;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Radoslav Husar
 * @author Paul Ferraro
 */
public class LegacyPropertyAddOperationTransformer implements OperationTransformer {

    private final Function<ModelNode, PathAddress> addressResolver;

    public LegacyPropertyAddOperationTransformer() {
        this(operation -> Operations.getPathAddress(operation));
    }

    public LegacyPropertyAddOperationTransformer(Function<ModelNode, PathAddress> addressResolver) {
        this.addressResolver = addressResolver;
    }

    @Override
    public ModelNode transformOperation(ModelNode operation) {
        if (operation.hasDefined(PROPERTIES)) {
            final ModelNode addOperation = operation.clone();
            List<Property> properties = addOperation.remove(PROPERTIES).asPropertyList();

            List<ModelNode> operations = new ArrayList<>(properties.size() + 1);
            operations.add(addOperation);

            PathAddress address = this.addressResolver.apply(addOperation);

            for (final Property property : properties) {
                String key = property.getName();
                ModelNode value = property.getValue();
                ModelNode propertyAddOperation = Util.createAddOperation(address.append(PathElement.pathElement(PROPERTY, key)));
                propertyAddOperation.get(VALUE).set(value);
                operations.add(propertyAddOperation);
            }
            return Operations.createCompositeOperation(operations);
        }
        return operation;
    }
}
