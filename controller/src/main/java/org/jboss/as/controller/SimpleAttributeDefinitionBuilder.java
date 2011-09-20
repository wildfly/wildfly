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

import java.util.EnumSet;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Provides a builder API for creating a {@link SimpleAttributeDefinition}.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class SimpleAttributeDefinitionBuilder {

    private final String name;
    private final ModelType type;
    private String xmlName;
    private boolean allowNull;
    private boolean allowExpression;
    private ModelNode defaultValue;
    private MeasurementUnit measurementUnit;
    private String[] alternatives;
    private String[] requires;
    private ParameterValidator validator;
    private AttributeAccess.Flag[] flags;

    public SimpleAttributeDefinitionBuilder(final String attributeName, final ModelType type) {
        this(attributeName, type, false);
    }

    public SimpleAttributeDefinitionBuilder(final String attributeName, final ModelType type, final boolean allowNull) {
        this.name = attributeName;
        this.type = type;
        this.allowNull = allowNull;
        this.xmlName = name;
    }

    public SimpleAttributeDefinition build() {
        return new SimpleAttributeDefinition(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                                     validator, alternatives, requires, flags);
    }

    public SimpleAttributeDefinitionBuilder setXmlName(String xmlName) {
        this.xmlName = xmlName == null ? this.name : xmlName;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setAllowExpression(boolean allowExpression) {
        this.allowExpression = allowExpression;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setDefaultValue(ModelNode defaultValue) {
        this.defaultValue = (defaultValue == null || !defaultValue.isDefined()) ? null : defaultValue;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setMeasurementUnit(MeasurementUnit unit) {
        this.measurementUnit = unit;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setValidator(ParameterValidator validator) {
        this.validator = validator;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setAlternatives(String... alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setRequires(String... requires) {
        this.requires = requires;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setFlags(AttributeAccess.Flag... flags) {
        this.flags = flags;
        return this;
    }


}
