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

import java.util.Arrays;
import java.util.Set;

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

    public static SimpleAttributeDefinitionBuilder create(final String name, final ModelType type) {
        return new SimpleAttributeDefinitionBuilder(name, type);
    }

    public static SimpleAttributeDefinitionBuilder create(final String name, final ModelType type, final boolean allowNull) {
        return new SimpleAttributeDefinitionBuilder(name, type, allowNull);
    }

    public static SimpleAttributeDefinitionBuilder create(final SimpleAttributeDefinition basis) {
        return new SimpleAttributeDefinitionBuilder(basis);
    }

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
    private boolean validateNull = true;

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

    public SimpleAttributeDefinitionBuilder(final SimpleAttributeDefinition basis) {
        this.name = basis.getName();
        this.type = basis.getType();
        this.xmlName = basis.getXmlName();
        this.allowNull = basis.isAllowNull();
        this.allowExpression = basis.isAllowExpression();
        this.defaultValue = basis.getDefaultValue();
        this.measurementUnit = basis.getMeasurementUnit();
        this.alternatives = basis.getAlternatives();
        this.requires = basis.getRequires();
        this.validator = basis.getValidator();
        Set<AttributeAccess.Flag> basisFlags = basis.getFlags();
        this.flags = basisFlags.toArray(new AttributeAccess.Flag[basisFlags.size()]);
    }

    public SimpleAttributeDefinition build() {
        return new SimpleAttributeDefinition(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                                     corrector, validator, validateNull, alternatives, requires, flags);
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

    public SimpleAttributeDefinitionBuilder setCorrector(ParameterCorrector corrector) {
        this.corrector = corrector;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setValidator(ParameterValidator validator) {
        this.validator = validator;
        return this;
    }

    /**
     * Sets whether the attribute definition should check for {@link ModelNode#isDefined() undefined} values if
     * {@link #setAllowNull(boolean) null is not allowed} in addition to any validation provided by any
     * {@link #setValidator(ParameterValidator) configured validator}. The default if not set is {@code true}. The use
     * case for setting this to {@code false} would be to ignore undefined values in the basic validation performed
     * by the {@link AttributeDefinition} and instead let operation handlers validate using more complex logic
     * (e.g. checking for {@link #setAlternatives(String...) alternatives}.
     *
     * @param validateNull {@code true} if additional validation should be performed; {@false} otherwise
     */
    public SimpleAttributeDefinitionBuilder setValidateNull(boolean validateNull) {
        this.validateNull = validateNull;
        return this;
    }

    public SimpleAttributeDefinitionBuilder setAlternatives(String... alternatives) {
        this.alternatives = alternatives;
        return this;
    }

    public SimpleAttributeDefinitionBuilder addAlternatives(String... alternatives) {
        if(this.alternatives == null) {
            this.alternatives = alternatives;
        } else {
            String[] newAlternatives = Arrays.copyOf(this.alternatives, this.alternatives.length + alternatives.length);
            System.arraycopy(alternatives, 0, newAlternatives, this.alternatives.length, alternatives.length);
            this.alternatives = newAlternatives;
        }
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

    public SimpleAttributeDefinitionBuilder addFlag(final AttributeAccess.Flag flag) {
        if(flags == null) {
            flags = new AttributeAccess.Flag[] { flag };
        } else {
            final int i = flags.length;
            flags = Arrays.copyOf(flags, i + 1);
            flags[i] = flag;
        }
        return this;
    }

    public SimpleAttributeDefinitionBuilder removeFlag(final AttributeAccess.Flag flag) {
        if (!isFlagPresent(flag))return this; //if not present no need to remove
        if (flags != null && flags.length > 0) {
            final int length = flags.length;
            final AttributeAccess.Flag[] newFlags = new AttributeAccess.Flag[length - 1];
            int k = 0;
            for (int i = 0; i < length; i++) {
                if (flags[i] != flag) {
                    newFlags[k] = flags[i];
                    k++;
                }
            }
            if (k != length - 1) {
                flags = newFlags;
            }
        }
        return this;
    }
    private boolean isFlagPresent(final AttributeAccess.Flag flag){
        if (flags==null)return false;
        for (AttributeAccess.Flag f: flags){
            if (f.equals(flag))return true;
        }
        return false;
    }

    public SimpleAttributeDefinitionBuilder setStorageRuntime() {
        removeFlag(AttributeAccess.Flag.STORAGE_CONFIGURATION);
        return addFlag(AttributeAccess.Flag.STORAGE_RUNTIME);
    }

    public SimpleAttributeDefinitionBuilder setRestartAllServices() {
        removeFlag(AttributeAccess.Flag.RESTART_NONE);
        removeFlag(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        removeFlag(AttributeAccess.Flag.RESTART_JVM);
        return addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    public SimpleAttributeDefinitionBuilder setRestartJVM() {
        removeFlag(AttributeAccess.Flag.RESTART_NONE);
        removeFlag(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        removeFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        return addFlag(AttributeAccess.Flag.RESTART_JVM);
    }

}
