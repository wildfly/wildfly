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

import org.jboss.as.controller.access.management.AccessConstraintDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Provides a builder API for creating an {@link org.jboss.as.controller.AttributeDefinition}.
 *
 * @param <BUILDER> the specific subclass type returned by the various builder API methods
 * @param <ATTRIBUTE> the type of {@link org.jboss.as.controller.AttributeDefinition} produced by the {@link #build()} method
 *
 * @author Tomaz Cerar
 */
@SuppressWarnings("unchecked")
public abstract class AbstractAttributeDefinitionBuilder<BUILDER extends AbstractAttributeDefinitionBuilder, ATTRIBUTE extends AttributeDefinition> {

    protected final String name;
    protected ModelType type;
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
    protected AccessConstraintDefinition[] accessConstraints;
    protected Boolean nullSignficant;
    protected AttributeParser parser;

    /**
     * Creates a builder for an attribute with the give name and type. Equivalent to
     * {@link #AbstractAttributeDefinitionBuilder(String, org.jboss.dmr.ModelType, boolean) AbstractAttributeDefinitionBuilder(attributeName, type, false}
     * @param attributeName the {@link AttributeDefinition#getName() name} of the attribute. Cannot be {@code null}
     * @param type the {@link AttributeDefinition#getType() type} of the attribute. Cannot be {@code null}
     */
    public AbstractAttributeDefinitionBuilder(final String attributeName, final ModelType type) {
        this(attributeName, type, false);
    }

    /**
     * Creates a builder for an attribute with the give name and type and nullability setting.
     * @param attributeName the {@link AttributeDefinition#getName() name} of the attribute. Cannot be {@code null}
     * @param type the {@link AttributeDefinition#getType() type} of the attribute. Cannot be {@code null}
     * @param allowNull {@code true} if the {@link AttributeDefinition#isAllowNull() allows undefined values}
     */
    public AbstractAttributeDefinitionBuilder(final String attributeName, final ModelType type, final boolean allowNull) {
        this.name = attributeName;
        this.type = type;
        this.allowNull = allowNull;
        this.xmlName = name;
    }

    /**
     * Creates a builder populated with the values of an existing attribute definition.
     *
     * @param basis the existing attribute definition. Cannot be {@code null}
     */
    public AbstractAttributeDefinitionBuilder(final AttributeDefinition basis) {
        this(null, basis);
    }

    /**
     * Creates a builder populated with the values of an existing attribute definition, with an optional
     * change of the attribute's name.
     *
     * @param attributeName the {@link AttributeDefinition#getName() name} of the attribute,
     *                      or {@code null} if the name from {@code basis} should be used
     * @param basis the existing attribute definition. Cannot be {@code null}
     */
    public AbstractAttributeDefinitionBuilder(final String attributeName, final AttributeDefinition basis) {
        this.name = attributeName != null ? attributeName : basis.getName();
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
        this.parser = basis.getParser();
    }

    /**
     * Create the {@link org.jboss.as.controller.AttributeDefinition}
     * @return the attribute definition. Will not return {@code null}
     */
    public abstract ATTRIBUTE build();

    /**
     * Sets the {@link AttributeDefinition#getXmlName() xml name} for the attribute, which is only needed
     * if the name used for the attribute is different from its ordinary
     * {@link AttributeDefinition#getName() name in the model}. If not set the default value is the name
     * passed to the builder constructor.
     *
     * @param xmlName the xml name. {@code null} is allowed
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setXmlName(String xmlName) {
        this.xmlName = xmlName == null ? this.name : xmlName;
        return (BUILDER) this;
    }

    /**
     * Sets whether the attribute should {@link AttributeDefinition#isAllowNull() allow undefined values}.
     * If not set the default value is the value provided to the builder constructor, or {@code false}
     * if no value is provided.
     *
     * @param allowNull {@code true} if undefined values should be allowed
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setAllowNull(boolean allowNull) {
        this.allowNull = allowNull;
        return (BUILDER) this;
    }

    /**
     * Sets whether the attribute should {@link AttributeDefinition#isAllowExpression() allow expressions}
     * If not set the default value is {@code false}.
     *
     * @param allowExpression {@code true} if expression values should be allowed
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setAllowExpression(boolean allowExpression) {
        this.allowExpression = allowExpression;
        return (BUILDER) this;
    }

    /**
     * Sets a {@link AttributeDefinition#getDefaultValue() default value} to use for the attribute if no
     * user-provided value is available.
     * @param defaultValue the default value, or {@code null} if no default should be used
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setDefaultValue(ModelNode defaultValue) {
        this.defaultValue = (defaultValue == null || !defaultValue.isDefined()) ? null : defaultValue;
        return (BUILDER) this;
    }

    /**
     * Sets a {@link AttributeDefinition#getMeasurementUnit() measurement unit} to describe the unit in
     * which a numeric attribute is expressed.
     * @param unit the unit. {@code null} is allowed
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setMeasurementUnit(MeasurementUnit unit) {
        this.measurementUnit = unit;
        return (BUILDER) this;
    }

    /**
     * Sets a {@link org.jboss.as.controller.ParameterCorrector} to use to adjust any user provided values
     * before {@link org.jboss.as.controller.AttributeDefinition#validateOperation(org.jboss.dmr.ModelNode, boolean) validation}
     * occurs.
     * @param corrector the corrector. May be {@code null}
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setCorrector(ParameterCorrector corrector) {
        this.corrector = corrector;
        return (BUILDER) this;
    }

    /**
     * Sets the validator that should be used to validate attribute values. The resulting attribute definition
     * will wrap this validator in one that enforces the attribute's
     * {@link AttributeDefinition#isAllowNull() allow null} and
     * {@link AttributeDefinition#isAllowExpression() allow expression} settings, so the given {@code validator}
     * need not be properly configured for those validations.
     * @param validator the validator. {@code null} is allowed
     * @return a builder that can be used to continue building the attribute definition
     */
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
     * @param validateNull {@code true} if additional validation should be performed; {@code false} otherwise
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setValidateNull(boolean validateNull) {
        this.validateNull = validateNull;
        return (BUILDER) this;
    }

    /**
     * Sets {@link AttributeDefinition#getAlternatives() names of alternative attributes} that should not
     * be defined if this attribute is defined.
     * @param alternatives the attribute names
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setAlternatives(String... alternatives) {
        this.alternatives = alternatives;
        return (BUILDER) this;
    }

    /**
     * Adds {@link AttributeDefinition#getAlternatives() names of alternative attributes} that should not
     * be defined if this attribute is defined.
     * @param alternatives the attribute names
     * @return a builder that can be used to continue building the attribute definition
     */
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

    /**
     * Sets {@link AttributeDefinition#getRequires() names of required attributes} that must
     * be defined if this attribute is defined.
     * @param requires the attribute names
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setRequires(String... requires) {
        this.requires = requires;
        return (BUILDER) this;
    }

    /**
     * Sets the {@link AttributeAccess.Flag special purpose flags} that are relevant to the attribute
     * @param flags the flags
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setFlags(AttributeAccess.Flag... flags) {
        this.flags = flags;
        return (BUILDER) this;
    }

    /**
     * Adds a {@link AttributeAccess.Flag special purpose flag} that is relevant to the attribute
     * @param flag the flag
     * @return a builder that can be used to continue building the attribute definition
     */
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

    /**
     * Removes a {@link AttributeAccess.Flag special purpose flag} from the set of those relevant to the attribute
     * @param flag the flag
     * @return a builder that can be used to continue building the attribute definition
     */
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

    /**
     * Checks if a {@link AttributeAccess.Flag special purpose flag} has been recorded as relevant to the attribute
     * @param flag the flag
     * @return a builder that can be used to continue building the attribute definition
     */
    protected boolean isFlagPresent(final AttributeAccess.Flag flag) {
        if (flags == null) { return false; }
        for (AttributeAccess.Flag f : flags) {
            if (f.equals(flag)) { return true; }
        }
        return false;
    }

    /**
     * Adds the {@link AttributeAccess.Flag#STORAGE_RUNTIME} flag and removes any conflicting flag.
     *
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setStorageRuntime() {
        removeFlag(AttributeAccess.Flag.STORAGE_CONFIGURATION);
        return addFlag(AttributeAccess.Flag.STORAGE_RUNTIME);
    }

    /**
     * Adds the {@link AttributeAccess.Flag#RESTART_ALL_SERVICES} flag and removes any conflicting flag.
     *
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setRestartAllServices() {
        removeFlag(AttributeAccess.Flag.RESTART_NONE);
        removeFlag(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        removeFlag(AttributeAccess.Flag.RESTART_JVM);
        return addFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES);
    }

    /**
     * Adds the {@link AttributeAccess.Flag#RESTART_JVM} flag and removes any conflicting flag.
     *
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setRestartJVM() {
        removeFlag(AttributeAccess.Flag.RESTART_NONE);
        removeFlag(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        removeFlag(AttributeAccess.Flag.RESTART_ALL_SERVICES);
        return addFlag(AttributeAccess.Flag.RESTART_JVM);
    }

    /**
     * Sets a maximum size for a collection-type attribute.
     * TODO this is not used by {@link org.jboss.as.controller.SimpleAttributeDefinition} even though
     * intuitively a user may expect it would be used.
     * @param maxSize the maximum size
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setMaxSize(final int maxSize) {
        this.maxSize = maxSize;
        return (BUILDER) this;
    }

    /**
     * Sets a minimum size description for a collection-type attribute.
     * TODO this is not used by {@link org.jboss.as.controller.SimpleAttributeDefinition} even though
     * intuitively a user may expect it would be used.
     * @param minSize the minimum size
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setMinSize(final int minSize) {
        this.minSize = minSize;
        return (BUILDER) this;
    }

    /**
     * Sets a custom {@link org.jboss.as.controller.AttributeMarshaller} to use for marshalling the attribute to xml.
     * If not set, a {@link org.jboss.as.controller.DefaultAttributeMarshaller} will be used.
     * @param marshaller the marshaller. Can be {@code null}
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setAttributeMarshaller(AttributeMarshaller marshaller) {
        this.attributeMarshaller = marshaller;
        return (BUILDER) this;
    }
    /**
     * Sets a custom {@link org.jboss.as.controller.AttributeParser} to use for parsing attribute from xml.
     * If not set, a {@link org.jboss.as.controller.AttributeParser#SIMPLE} will be used.
     * @param parser the parser. Can be {@code null}
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setAttributeParser(AttributeParser parser) {
        this.parser = parser;
        return (BUILDER) this;
    }

    /**
     * Marks an attribute as only relevant to a resource, and not a valid parameter to an "add" operation that
     * creates that resource. Typically used for legacy "name" attributes that display the final value in the
     * resource's address as an attribute.
     *
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setResourceOnly() {
        this.resourceOnly = true;
        return (BUILDER) this;
    }

    /**
     * Marks the attribute as deprecated since the given API version.
     * @param since the API version, with the API being the one (core or a subsystem) in which the attribute is used
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setDeprecated(ModelVersion since) {
        this.deprecated = new DeprecationData(since);
        return (BUILDER) this;
    }

    /**
     * Sets access constraints to use with the attribute
     * @param accessConstraints the constraints
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setAccessConstraints(AccessConstraintDefinition... accessConstraints) {
        this.accessConstraints = accessConstraints;
        return (BUILDER) this;
    }

    /**
     * Adds an access constraint to the set used with the attribute
     * @param accessConstraint the constraint
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER addAccessConstraint(final AccessConstraintDefinition accessConstraint) {
        if (accessConstraints == null) {
            accessConstraints = new AccessConstraintDefinition[] {accessConstraint};
        } else {
            accessConstraints = Arrays.copyOf(accessConstraints, accessConstraints.length + 1);
            accessConstraints[accessConstraints.length - 1] = accessConstraint;
        }
        return (BUILDER) this;
    }

    /**
     * Sets whether an access control check is required to implicitly set an attribute to {@code undefined}
     * in a resource "add" operation. "Implicitly" setting an attribute refers to not providing a value for
     * it in the add operation, leaving the attribute in an undefined state. If not set
     * the default value is whether the attribute {@link AttributeDefinition#isAllowNull() allows null} and
     * has a {@link AttributeDefinition#getDefaultValue() default value}.
     *
     * @param nullSignficant {@code true} if an undefined value is significant
     * @return a builder that can be used to continue building the attribute definition
     */
    public BUILDER setNullSignficant(boolean nullSignficant) {
        this.nullSignficant = nullSignficant;
        return (BUILDER) this;
    }
}
