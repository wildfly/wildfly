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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.COMPOSITE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.STEPS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;

/**
 * @author Radoslav Husar
 * @version October 2015
 */
public class LegacyPropertyAddOperationTransformer implements OperationTransformer {

    @Override
    public ModelNode transformOperation(ModelNode operation) {
        if (operation.hasDefined(PROPERTIES)) {
            final ModelNode addOp = operation.clone();
            final ModelNode properties = addOp.remove(PROPERTIES);

            final ModelNode composite = new ModelNode();
            composite.get(OP).set(COMPOSITE);
            composite.get(OP_ADDR).setEmptyList();
            composite.get(STEPS).add(addOp);

            // Handle odd jgroups-specific legacy case, where :add operation for the protocol is :add-protocol on the parent
            PathAddress propertyAddress = Operations.getName(addOp).equals("add-protocol")
                    ? Operations.getPathAddress(addOp).append("protocol", addOp.get("type").asString())
                    : Operations.getPathAddress(addOp);

            for (final Property property : properties.asPropertyList()) {
                String key = property.getName();
                ModelNode value = property.getValue();
                ModelNode propAddOp = Util.createAddOperation(propertyAddress.append(PathElement.pathElement(PROPERTY, key)));
                propAddOp.get(VALUE).set(value);
                composite.get(STEPS).add(propAddOp);
            }
            return composite;
        }
        return operation;
    }
}
