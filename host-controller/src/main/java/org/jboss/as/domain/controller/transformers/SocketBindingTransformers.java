/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.domain.controller.transformers;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;

import java.util.HashMap;
import java.util.Map;

import org.jboss.as.controller.resource.AbstractSocketBindingResourceDefinition;
import org.jboss.as.controller.transform.AttributeTransformationRequirementChecker;
import org.jboss.as.controller.transform.RejectExpressionValuesTransformer;
import org.jboss.as.controller.transform.ResourceTransformer;
import org.jboss.as.controller.transform.TransformersSubRegistration;
import org.jboss.as.domain.controller.resources.SocketBindingResourceDefinition;

/**
 * The older versions of the model do not allow expressions for some socket-binding resource attributes.
 * Reject the attributes if they contain an expression.
 *
 * @author Brian Stansberry (c) 2012 Red Hat Inc.
 */
class SocketBindingTransformers {

    static TransformersSubRegistration registerTransformers(TransformersSubRegistration parent) {

        Map<String, AttributeTransformationRequirementChecker> fieldCheckers = new HashMap<String, AttributeTransformationRequirementChecker>();
        fieldCheckers.put(AbstractSocketBindingResourceDefinition.CLIENT_MAPPING_SOURCE_NETWORK.getName(), AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);
        fieldCheckers.put(AbstractSocketBindingResourceDefinition.CLIENT_MAPPING_DESTINATION_ADDRESS.getName(), AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);
        fieldCheckers.put(AbstractSocketBindingResourceDefinition.CLIENT_MAPPING_DESTINATION_PORT.getName(), AttributeTransformationRequirementChecker.SIMPLE_EXPRESSIONS);

        AttributeTransformationRequirementChecker elementChecker = new AttributeTransformationRequirementChecker.ObjectFieldsAttributeTransformationRequirementChecker(fieldCheckers);
        AttributeTransformationRequirementChecker attrChecker = new AttributeTransformationRequirementChecker.ListAttributeTransformationRequirementChecker(elementChecker);
        RejectExpressionValuesTransformer rejectExpression = new RejectExpressionValuesTransformer(SocketBindingResourceDefinition.CLIENT_MAPPINGS.getName(), attrChecker);
        TransformersSubRegistration reg = parent.registerSubResource(SocketBindingResourceDefinition.PATH, (ResourceTransformer) rejectExpression);

        reg.registerOperationTransformer(ADD, rejectExpression);
        reg.registerOperationTransformer(WRITE_ATTRIBUTE_OPERATION, rejectExpression.getWriteAttributeTransformer());

        return reg;
    }
}
