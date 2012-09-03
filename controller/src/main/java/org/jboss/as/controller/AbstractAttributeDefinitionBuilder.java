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
 * Provides a builder API for creating a
 *
 * @author Tomaz Cerar
 */
public abstract class AbstractAttributeDefinitionBuilder<BUILDER extends AbstractAttributeDefinitionBuilder, ATTRIBUTE extends AttributeDefinition> {

    protected final String name;
    protected final ModelType type;
    protected String xmlName;
    protected boolean allowNull;
    protected boolean allowExpression;
    protected ModelNode defaultValue;
    protected MeasurementUnit measurementUnit;
    protected String[] alternatives;
    protected String[] requires;
    protected ParameterCorrector corrector;
    protected ParameterValidator validator;
    protected boolean validateNull = true;
    protected int minSize = 0;
    protected int maxSize = Integer.MAX_VALUE;
    protected AttributeAccess.Flag[] flags;
    protected AttributeMarshaller attributeMarshaller = null;
    protected boolean resourceOnly = false;
    protected DeprecationData deprecated = null;

    public AbstractAttributeDefinitionBuilder(final String attributeName, final ModelType type) {
        this(attributeName, type, false);
    }

    public AbstractAttributeDefinitionBuilder(final String attributeName, final ModelType type, final boolean allowNull) {
        this.name = attributeName;
        this.type = type;
        this.allowNull = allowNull;
        this.xmlName = name;
    }

    public AbstractAttributeDefinitionBuilder(final AttributeDefinition basis) {
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
        this.attributeMarshaller = basis.getAttributeMarshaller();
    }

    public abstract ATTRIBUTE build();

    public BUILDER setXmlName(String xmlName) {
        this.xmlName = xmlName == null ? this.name : xmlName;
        return (BUILDER) this;
    }

    public BUILDER setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
        return (BUILDER) this;
    }

    public BUILDER setAllowExpression(boolean allowExpression) {
        this.allowExpression = allowExpression;
        return (BUILDER) this;
    }

    public BUILDER setDefaultValue(ModelNode defaultValue) {
        this.defaultValue = (defaultValue == null || !defaultValue.isDefined()) ? null : defaultValue;
        return (BUILDER) this;
    }

    public BUILDER setMeasurementUnit(MeasurementUnit unit) {
        this.measurementUnit = unit;
        return (BUILDER) this;
    }

    public BUILDER setCorrector(ParameterCorrector corrector) {
        this.corrector = corrector;
        return (BUILDER) this;
    }

    public BUILDER setValidator(ParameterValidator validator) {
        this.validator = validator;
        return (BUILDER) this;
    }

    /**
     * Sets whether the attribute definition should check for {@link org.jboss.dmr.ModelNode#isDefined() undefined} values if
     * {@link #setAllowNull(boolean) null is not allowed} in addition to any validation provided by any
     * {@link #setValidator(org.jboss.as.controller.operations.validation.ParameterValidator) configured validator}. The default if not set is {@code true}. The use
     * case for setting this to {@code false} would be to ignore undefined values in the basic validation performed
     * by the {@link org.jboss.as.controller.AttributeDefinition} and instead let operation handlers validate using more complex logic
     * (e.g. checking for {@link #setAlternatives(String...) alternatives}.
     *
     * @param validateNull {@code true} if additional validation should be performed; {@false} otherwise
     */
    public BUILDER setValidateNull(boolean validateNull) {
        this.validateNull = validateNull;
        return (BUILDER) this;
    }

    public BUILDER setAlternatives(String... alternatives) {
        this.alternatives = alternatives;
        return (BUILDER) this;
    }

    public BUILDER addAlternatives(String... alternatives) {
        if (this.alternatives == null) {
            this.alternatives = alternatives;
        } else {
            String[] newAlternatives = Arrays.copyOf(this.alternatives, this.alternatives.length + alternatives.length);
            System.arraycopy(alternatives, 0, newAlternatives, this.alternatives.length, alternatives.length);
            this.alternatives = newAlternatives;
        }
        return (BUILDER) this;
    }

    public BUILDER setRequires(String... requires) {
        this.requires = requires;
        return (BUILDER) this;
    }

    public BUILDER setFlags(AttributeAccess.Flag... flags) {
        this.flags = flags;
        return (BUILDER) this;
    }

    public BUILDER addFlag(final AttributeAccess.Flag flag) {
        if (flags == null) {
            flags = new AttributeAccess.Flag[]{flag};
        } else {
            final int i = flags.length;
            flags = Arrays.copyOf(flags, i + 1);
            flags[i] = flag;
        }
        return (BUILDER) this;
    }

    public BUILDER removeFlag(final AttributeAccess.Flag flag) {
        if (!isFlagPresent(flag)) {
            return (BUILDER) this; //if not present no need to remove
        }
        if (flags != null && flags.length > 0) {
            final int length = flags.length;
            final AttributeAccess.Flag[] newFlags = new AttributeAccess.Flag[length - 1];
            int k = 0;
            for (AttributeAccess.Flag flag1 : flags) {
                if (flag1 != flag) {
                    newFlags[k] = flag1;
                    k++;
                }
            }
            if (k != length - 1) {
                flags = newFlags;
            }
        }
        return (BUILDER) this;
    }

    protected boolean isFlagPresent(final AttributeAccess.Flag flag) {
        if (flags == null) { return false; }
        for (AttributeAccess.Flag f : flags) {
            if (f.equals(flag)) { return true; }
        }
        return false;
    }

    public BUILDER setStorageRuntime() {
        removeFlag(AttributeAccess.Flag.STORAGE_CONFIGURATION);
        return addFlag(AttributeAccess.Flag.STORAGE_RUNTIME);
    }

    public BUILDER setRestartAllServices() {
        removeFlag(AttributeAccess.Flag.RESTART_NONE);
        removeFlag(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        removeFlag(AttributeAccess.Flag.RESTART_JVM);
        return addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    public BUILDER setRestartJVM() {
        removeFlag(AttributeAccess.Flag.RESTART_NONE);
        removeFlag(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        removeFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        return addFlag(AttributeAccess.Flag.RESTART_JVM);
    }

    public BUILDER setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
        return (BUILDER) this;
    }

    public BUILDER setMinSize(final int minSize) {
        this.minSize = minSize;
        return (BUILDER) this;
    }

    public BUILDER setAttributeMarshaller(AttributeMarshaller marshaller) {
        this.attributeMarshaller = marshaller;
        return (BUILDER) this;
    }

    public BUILDER setResourceOnly() {
        this.resourceOnly = true;
        return (BUILDER) this;
    }

    public BUILDER setDeprecated(ModelVersion since) {
        this.deprecated = new DeprecationData(since);
        return (BUILDER) this;
    }
}
