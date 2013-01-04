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

package org.jboss.as.controller;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class PrimitiveListAttributeDefinition extends ListAttributeDefinition {
    private final ModelType valueType;

    @Deprecated
    protected PrimitiveListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions, final ModelType valueType, final int minSize, final int maxSize,
                                               final String[] alternatives, final String[] requires, final AttributeMarshaller attributeMarshaller, final boolean resourceOnly,
                                               final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, allowExpressions, minSize, maxSize, new ModelTypeValidator(valueType), alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        this.valueType = valueType;
    }

    protected PrimitiveListAttributeDefinition(final String name, final String xmlName, final boolean allowNull, final boolean allowExpressions, final ModelType valueType, final int minSize, final int maxSize,
                                               final String[] alternatives, final String[] requires, ParameterValidator elementValidator, final AttributeMarshaller attributeMarshaller,
                                               final boolean resourceOnly, final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, allowNull, allowExpressions, minSize, maxSize, elementValidator, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        this.valueType = valueType;
    }

    public ModelType getValueType() {
        return valueType;
    }

    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
        final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
        addValueTypeDescription(result);
        return result;
    }

    @Override
    protected void addValueTypeDescription(final ModelNode node, final ResourceBundle bundle) {
        addValueTypeDescription(node);
    }


    protected void addValueTypeDescription(final ModelNode node) {
        if (isAllowExpression()) {
            node.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(true);
        }
        node.get(ModelDescriptionConstants.VALUE_TYPE).set(valueType);
    }

    @Override
    protected ModelNode convertParameterElementExpressions(ModelNode parameterElement) {
        if (isAllowExpression() && COMPLEX_TYPES.contains(valueType)) {
            // This implementation isn't suitable. Must be overridden
            throw new IllegalStateException();
        }
        return super.convertParameterElementExpressions(parameterElement);
    }

    @Override
    protected void addAttributeValueTypeDescription(final ModelNode node, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        addValueTypeDescription(node);
    }

    @Override
    protected void addOperationParameterValueTypeDescription(final ModelNode node, final String operationName, final ResourceDescriptionResolver resolver, final Locale locale, final ResourceBundle bundle) {
        addValueTypeDescription(node);
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, PrimitiveListAttributeDefinition> {
        private final ModelType valueType;


        public Builder(final String name, final ModelType valueType) {
            super(name, ModelType.LIST);
            this.valueType = valueType;
        }

        public Builder(final PrimitiveListAttributeDefinition basic) {
            super(basic);
            this.valueType = basic.getValueType();
        }

        public static Builder of(final String name, final ModelType valueType) {
            return new Builder(name, valueType);
        }

        public PrimitiveListAttributeDefinition build() {
            if (xmlName == null) { xmlName = name; }
            if (maxSize < 1) { maxSize = Integer.MAX_VALUE; }
            if (validator == null) {
                validator = new ModelTypeValidator(valueType, allowNull, allowExpression);
            }
            return new PrimitiveListAttributeDefinition(name, xmlName, allowNull, allowExpression, valueType, minSize, maxSize, alternatives, requires, validator, attributeMarshaller, resourceOnly, deprecated, flags);
        }
    }
}
