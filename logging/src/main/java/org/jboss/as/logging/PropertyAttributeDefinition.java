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

package org.jboss.as.logging;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.config.PropertyConfigurable;

/**
 * Defines an attribute with a property name.
 */
public class PropertyAttributeDefinition extends SimpleAttributeDefinition implements ConfigurationProperty<String> {
    private final ModelNodeResolver<String> resolver;
    private final String propertyName;

    /** @deprecated Use {@link Builder} */
    @Deprecated
    public PropertyAttributeDefinition(final String name, final String xmlName, final String propertyName, final ModelNodeResolver<String> resolver, final ModelNode defaultValue, final ModelType type,
                                       final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit, final ParameterCorrector corrector,
                                       final ParameterValidator validator, final boolean validateNull, final String[] alternatives, final String[] requires,
                                       final AttributeMarshaller attributeMarshaller, final boolean resourceOnly, final DeprecationData deprecationData, final Flag... flags) {
        this(name, xmlName, propertyName, resolver, defaultValue, type, allowNull, allowExpression, measurementUnit, corrector, validator, validateNull, alternatives, requires, attributeMarshaller,
                resourceOnly, deprecationData, null, null, flags);
    }

    private PropertyAttributeDefinition(final String name, final String xmlName, final String propertyName, final ModelNodeResolver<String> resolver, final ModelNode defaultValue, final ModelType type,
            final boolean allowNull, final boolean allowExpression, final MeasurementUnit measurementUnit, final ParameterCorrector corrector,
            final ParameterValidator validator, final boolean validateNull, final String[] alternatives, final String[] requires,
            final AttributeMarshaller attributeMarshaller, final boolean resourceOnly, final DeprecationData deprecationData,
            final AccessConstraintDefinition[] accessConstraints, Boolean nullSignificant, final Flag... flags) {
        super(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit, corrector, validator, validateNull, alternatives, requires, attributeMarshaller,
                resourceOnly, deprecationData, accessConstraints, nullSignificant, flags);
        this.propertyName = propertyName;
        this.resolver = resolver;
    }

    @Override
    public void setPropertyValue(final OperationContext context, final ModelNode model, final PropertyConfigurable configuration) throws OperationFailedException {
        final String value = resolvePropertyValue(context, model);
        if (value == null) {
            configuration.removeProperty(propertyName);
        } else {
            configuration.setPropertyValueString(propertyName, value);
        }
    }

    @Override
    public ModelNodeResolver<String> resolver() {
        return resolver;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public String resolvePropertyValue(final OperationContext context, final ModelNode model) throws OperationFailedException {
        String result = null;
        final ModelNode value = resolveModelAttribute(context, model);
        if (value.isDefined()) {
            if (resolver == null) {
                result = value.asString();
            } else {
                result = resolver.resolveValue(context, value);
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = 17;
        hash = prime * hash + (propertyName == null ? 0 : propertyName.hashCode());
        return hash;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof PropertyAttributeDefinition)) {
            return false;
        }
        final PropertyAttributeDefinition other = (PropertyAttributeDefinition) obj;
        return (propertyName == null ? other.propertyName == null : propertyName.equals(other.propertyName));
    }

    @Override
    public String toString() {
        return String.format("%s{propertyName=%s,attributeName=%s}", getClass().getName(), propertyName, getName());
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, PropertyAttributeDefinition> {

        private ModelNodeResolver<String> resolver;
        private String propertyName;

        Builder(final String name, final ModelType type) {
            super(name, type);
        }

        /**
         * Creates a builder for {@link PropertyAttributeDefinition}.
         *
         * @param name the name of the attribute
         * @param type the attribute type
         *
         * @return a builder
         */
        public static Builder of(final String name, final ModelType type) {
            return new Builder(name, type);
        }

        /**
         * Creates a builder for {@link PropertyAttributeDefinition}.
         *
         * @param name      the name of the attribute
         * @param type      the attribute type
         * @param allowNull {@code true} if {@code null} is allowed, otherwise {@code false}
         *
         * @return a builder
         */
        public static Builder of(final String name, final ModelType type, final boolean allowNull) {
            return new Builder(name, type).setAllowNull(allowNull);
        }

        public PropertyAttributeDefinition build() {
            if (xmlName == null) xmlName = name;
            if (propertyName == null) propertyName = name;
            return new PropertyAttributeDefinition(name, xmlName, propertyName, resolver, defaultValue, type, allowNull, allowExpression, measurementUnit,
                    corrector, validator, validateNull, alternatives, requires, attributeMarshaller, resourceOnly,
                    deprecated, accessConstraints, nullSignficant, flags);
        }

        public Builder setPropertyName(final String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        public Builder setResolver(final ModelNodeResolver<String> resolver) {
            this.resolver = resolver;
            return this;
        }
    }
}
