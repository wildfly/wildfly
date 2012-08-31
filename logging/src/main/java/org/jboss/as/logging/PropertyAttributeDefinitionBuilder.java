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

import java.util.Arrays;

import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Builds an implementation of a {@link PropertyAttributeDefinition}
 */
public class PropertyAttributeDefinitionBuilder {

    private final String name;
    private final ModelType type;
    private String xmlName;
    private boolean allowNull;
    private boolean allowExpression;
    private ModelNode defaultValue;
    private MeasurementUnit measurementUnit;
    private String[] alternatives;
    private String[] requires;
    private ParameterCorrector corrector;
    private ParameterValidator validator;
    private AttributeAccess.Flag[] flags;
    private boolean validateNull = true;
    private ModelNodeResolver<String> resolver;
    private String propertyName;

    PropertyAttributeDefinitionBuilder(final String name, final ModelType type) {
        this.name = name;
        this.type = type;
    }

    /**
     * Creates a builder for {@link PropertyAttributeDefinition}.
     *
     * @param name the name of the attribute
     * @param type the attribute type
     *
     * @return a builder
     */
    public static PropertyAttributeDefinitionBuilder of(final String name, final ModelType type) {
        return new PropertyAttributeDefinitionBuilder(name, type);
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
    public static PropertyAttributeDefinitionBuilder of(final String name, final ModelType type, final boolean allowNull) {
        return new PropertyAttributeDefinitionBuilder(name, type).setAllowNull(allowNull);
    }

    public PropertyAttributeDefinition build() {
        if (xmlName == null) xmlName = name;
        if (propertyName == null) propertyName = name;
        return new PropertyAttributeDefinition(name, xmlName, propertyName, resolver, defaultValue, type, allowNull, allowExpression, measurementUnit,
                corrector, validator, validateNull, alternatives, requires, flags);
    }


    public PropertyAttributeDefinitionBuilder setXmlName(final String xmlName) {
        this.xmlName = xmlName == null ? this.name : xmlName;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setAllowNull(final boolean allowNull) {
        this.allowNull = allowNull;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setAllowExpression(final boolean allowExpression) {
        this.allowExpression = allowExpression;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setDefaultValue(final ModelNode defaultValue) {
        this.defaultValue = (defaultValue == null || !defaultValue.isDefined()) ? null : defaultValue;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setMeasurementUnit(MeasurementUnit unit) {
        this.measurementUnit = unit;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setCorrector(ParameterCorrector corrector) {
        this.corrector = corrector;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setValidator(final ParameterValidator validator) {
        this.validator = validator;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setAlternatives(final String... alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public PropertyAttributeDefinitionBuilder addAlternatives(final String... alternatives) {
        if (this.alternatives == null) {
            this.alternatives = alternatives;
        } else {
            String[] newAlternatives = Arrays.copyOf(this.alternatives, this.alternatives.length + alternatives.length);
            System.arraycopy(alternatives, 0, newAlternatives, this.alternatives.length, alternatives.length);
            this.alternatives = newAlternatives;
        }
        return this;
    }

    public PropertyAttributeDefinitionBuilder setRequires(final String... requires) {
        this.requires = requires;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setFlags(final AttributeAccess.Flag... flags) {
        this.flags = flags;
        return this;
    }

    /**
     * Sets whether the attribute definition should check for {@link ModelNode#isDefined() undefined} values if
     * {@link #setAllowNull(boolean) null is not allowed} in addition to any validation provided by any
     * {@link #setValidator(ParameterValidator) configured validator}. The default if not set is {@code true}. The
     * use
     * case for setting this to {@code false} would be to ignore undefined values in the basic validation performed
     * by the {@link PropertyAttributeDefinition} and instead let operation handlers validate using more complex
     * logic
     * (e.g. checking for {@link #setAlternatives(String...) alternatives}.
     *
     * @param validateNull {@code true} if additional validation should be performed; {@code false} otherwise
     */
    public PropertyAttributeDefinitionBuilder setValidateNull(boolean validateNull) {
        this.validateNull = validateNull;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setPropertyName(final String propertyName) {
        this.propertyName = propertyName;
        return this;
    }

    public PropertyAttributeDefinitionBuilder setResolver(final ModelNodeResolver<String> resolver) {
        this.resolver = resolver;
        return this;
    }
}
