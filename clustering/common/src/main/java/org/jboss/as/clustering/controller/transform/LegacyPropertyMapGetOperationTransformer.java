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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PROPERTY;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.READ_ATTRIBUTE_OPERATION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;

import org.jboss.as.clustering.controller.Operations;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.dmr.ModelNode;

/**
 * @author Radoslav Husar
 * @version October 2015
 */
public class LegacyPropertyMapGetOperationTransformer implements OperationTransformer {

    @Override
    public ModelNode transformOperation(ModelNode operation) {
        if (operation.get(NAME).asString().equals(PROPERTIES)) {
            String key = operation.get("key").asString();
            PathAddress address = Operations.getPathAddress(operation);
            ModelNode transformedOperation = Util.createOperation(READ_ATTRIBUTE_OPERATION, address.append(PathElement.pathElement(PROPERTY, key)));
            transformedOperation.get(NAME).set(VALUE);
            return transformedOperation;
        }
        return operation;
    }
}
