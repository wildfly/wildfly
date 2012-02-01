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

package org.jboss.as.clustering.subsystem.validators;

// import static org.jboss.as.clustering.ClusteringMessages.MESSAGES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
* Date: 16.11.2011
*
* @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
*/
public class ObjectTypeValidator extends ModelTypeValidator implements AllowedValuesValidator {
    private final Map<String, AttributeDefinition> allowedValues;
    private final List<ModelNode> nodeValues;

    public ObjectTypeValidator(final boolean nullable, final AttributeDefinition... attributes) {
        super(nullable, false, false, ModelType.OBJECT, findModelTypes(attributes));
        allowedValues = new HashMap<String, AttributeDefinition>(attributes.length);
        nodeValues = new ArrayList<ModelNode>(attributes.length);
        for (AttributeDefinition attribute : attributes) {
            allowedValues.put(attribute.getName(), attribute);
            nodeValues.add(new ModelNode().set(attribute.getName()));
        }
    }

    @Override
    public void validateParameter(final String parameterName, final ModelNode value) throws OperationFailedException {
        super.validateParameter(parameterName, value);
        if (value.isDefined()) {
            for (String key : value.keys()) {
                if (allowedValues.containsKey(key)) {
                    allowedValues.get(key).getValidator().validateParameter(key, value.get(key));
                } else {
                    // create and pass a two parameter message about this error
                    // throw new OperationFailedException(new ModelNode().set(MESSAGES.invalidValueTypeKey(key, allowedValues.keySet())));
                    throw new OperationFailedException(new ModelNode().set("invalid type key"));
                }
            }
        }
    }

    @Override
    public List<ModelNode> getAllowedValues() {
        return nodeValues;
    }

    private static ModelType[] findModelTypes(final AttributeDefinition... attributes) {
        final Set<ModelType> result = new HashSet<ModelType>();
        for (AttributeDefinition attr : attributes) {
            if (attr.getType() != ModelType.OBJECT)
                result.add(attr.getType());
        }
        return result.toArray(new ModelType[]{});
    }
}