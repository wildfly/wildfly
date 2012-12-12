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

package org.jboss.as.controller;

import java.util.LinkedList;
import java.util.List;

import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public final class StringListAttributeDefinition extends PrimitiveListAttributeDefinition {

    private StringListAttributeDefinition(final String name, final String xmlName, final boolean allowNull,final boolean allowExpressions, final int minSize, final int maxSize, final String[] alternatives,
                                          final String[] requires, ParameterValidator elementValidator, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly,
                                          final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, allowExpressions, ModelType.STRING, minSize, maxSize, alternatives, requires, elementValidator, attributeMarshaller, resourceOnly, deprecated, flags);
    }


    public List<String> unwrap(final OperationContext context, final ModelNode model) throws OperationFailedException {
        if (!model.hasDefined(getName())) {
            return null;
        }
        ModelNode modelProps = model.get(getName());
        List<String> result = new LinkedList<String>();
        for (ModelNode p : modelProps.asList()) {
            result.add(context.resolveExpressions(p).asString());
        }
        return result;
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, StringListAttributeDefinition> {
        public Builder(final String name) {
            super(name, ModelType.STRING);
            validator = new ModelTypeValidator(ModelType.STRING);
        }

        public Builder(final StringListAttributeDefinition basic) {
            super(basic);
            if (validator==null){
                validator = new ModelTypeValidator(ModelType.STRING);
            }
        }

        @Override
        public StringListAttributeDefinition build() {
            return new StringListAttributeDefinition(name, xmlName, allowNull, allowExpression, minSize, maxSize, alternatives, requires, validator, attributeMarshaller, resourceOnly, deprecated, flags);
        }
    }
}
