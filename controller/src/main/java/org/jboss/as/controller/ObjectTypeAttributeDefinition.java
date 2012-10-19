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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;
import java.util.ResourceBundle;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.MinMaxValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Date: 15.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 * @author Richard Achmatowicz (c) 2012 RedHat Inc.
 */
public class ObjectTypeAttributeDefinition extends SimpleAttributeDefinition {
    private final AttributeDefinition[] valueTypes;
    private final String suffix;

    /*
     * Constructor which allows specifying a custom ParameterValidator. Disabled by default.
     */
    protected ObjectTypeAttributeDefinition(final String name, final String xmlName, final String suffix, final AttributeDefinition[] valueTypes, final boolean allowNull,
                                            final ParameterValidator validator, final ParameterCorrector corrector, final String[] alternatives, final String[] requires,
                                            final AttributeMarshaller attributeMarshaller, final boolean resourceOnly, final DeprecationData deprecated, final AttributeAccess.Flag... flags) {
        super(name, xmlName, null, ModelType.OBJECT, allowNull, false, null, corrector, validator, false, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        this.valueTypes = valueTypes;
        if (suffix == null) {
            this.suffix = "";
        } else {
            this.suffix = suffix;
        }
    }


    @Override
    public ModelNode parse(final String value, final XMLStreamReader reader) throws XMLStreamException {
        throw new UnsupportedOperationException();
    }


    @Override
    public ModelNode addResourceAttributeDescription(ResourceBundle bundle, String prefix, ModelNode resourceDescription) {
        final ModelNode result = super.addResourceAttributeDescription(bundle, prefix, resourceDescription);
        addValueTypeDescription(result, prefix, bundle, null, null);
        return result;
    }

    public ModelNode addOperationParameterDescription(final ModelNode resourceDescription, final String operationName,
                                                      final ResourceDescriptionResolver resolver,
                                                      final Locale locale, final ResourceBundle bundle) {
        final ModelNode result = super.addOperationParameterDescription(resourceDescription, operationName, resolver, locale, bundle);
        addValueTypeDescription(result, getName(), bundle, resolver, locale);
        return result;
    }

    public ModelNode addResourceAttributeDescription(final ModelNode resourceDescription, final ResourceDescriptionResolver resolver,
                                                     final Locale locale, final ResourceBundle bundle) {
        final ModelNode result = super.addResourceAttributeDescription(resourceDescription, resolver, locale, bundle);
        addValueTypeDescription(result, getName(), bundle, resolver, locale);
        return result;
    }


    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
        final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);
        addValueTypeDescription(result, prefix, bundle, null, null);
        return result;
    }

    protected void addValueTypeDescription(final ModelNode node, final String prefix, final ResourceBundle bundle,
                                           final ResourceDescriptionResolver resolver,
                                           final Locale locale) {
        for (AttributeDefinition valueType : valueTypes) {
            // get the value type description of the attribute
            final ModelNode valueTypeDesc = getValueTypeDescription(valueType, false);
            final String p;
            boolean prefixUnuseable = prefix == null || prefix.isEmpty() ;
            boolean suffixUnuseable = suffix == null || suffix.isEmpty() ;

            if (prefixUnuseable && !suffixUnuseable) {
                p = suffix;
            } else if (!prefixUnuseable && suffixUnuseable) {
                p = prefix;
            } else {
                p = String.format("%s.%s", prefix, suffix);
            }

            // get the text description of the attribute
            if (resolver != null) {
                final String key = String.format("%s.%s", p, valueType.getName());
                valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).set(resolver.getResourceAttributeDescription(key, locale, bundle));
            } else {
                valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).set(valueType.getAttributeTextDescription(bundle, p));
            }
            // set it as one of our value types, and return the value
            final ModelNode childType = node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
            // if it is of type OBJECT itself (add its nested descriptions)
            // seeing that OBJECT represents a grouping, use prefix+"."+suffix for naming the entries
            if (valueType instanceof ObjectTypeAttributeDefinition) {
                ObjectTypeAttributeDefinition.class.cast(valueType).addValueTypeDescription(childType, p, bundle, resolver, locale);
            }
            // if it is of type LIST, and its value type
            // seeing that LIST represents a grouping, use prefix+"."+suffix for naming the entries
            if (valueType instanceof SimpleListAttributeDefinition) {
                SimpleListAttributeDefinition.class.cast(valueType).addValueTypeDescription(childType, p, bundle);
            }
        }
    }

    @Override
    public void marshallAsElement(final ModelNode resourceModel, final boolean marshalDefault, final XMLStreamWriter writer) throws XMLStreamException {
        if (resourceModel.hasDefined(getName())) {
            writer.writeStartElement(getXmlName());
            for (AttributeDefinition valueType : valueTypes) {
                for (ModelNode handler : resourceModel.get(getName()).asList()) {
                    valueType.marshallAsElement(handler, writer);
                }
            }
            writer.writeEndElement();
        }
    }

    private ModelNode getValueTypeDescription(final AttributeDefinition valueType, final boolean forOperation) {
        final ModelNode result = new ModelNode();
        result.get(ModelDescriptionConstants.TYPE).set(valueType.getType());
        result.get(ModelDescriptionConstants.DESCRIPTION); // placeholder
        result.get(ModelDescriptionConstants.EXPRESSIONS_ALLOWED).set(valueType.isAllowExpression());
        if (forOperation) {
            result.get(ModelDescriptionConstants.REQUIRED).set(!valueType.isAllowNull());
        }
        result.get(ModelDescriptionConstants.NILLABLE).set(valueType.isAllowNull());
        final ModelNode defaultValue = valueType.getDefaultValue();
        if (!forOperation && defaultValue != null && defaultValue.isDefined()) {
            result.get(ModelDescriptionConstants.DEFAULT).set(defaultValue);
        }
        MeasurementUnit measurementUnit = valueType.getMeasurementUnit();
        if (measurementUnit != null && measurementUnit != MeasurementUnit.NONE) {
            result.get(ModelDescriptionConstants.UNIT).set(measurementUnit.getName());
        }
        final String[] alternatives = valueType.getAlternatives();
        if (alternatives != null) {
            for (final String alternative : alternatives) {
                result.get(ModelDescriptionConstants.ALTERNATIVES).add(alternative);
            }
        }
        final String[] requires = valueType.getRequires();
        if (requires != null) {
            for (final String required : requires) {
                result.get(ModelDescriptionConstants.REQUIRES).add(required);
            }
        }
        final ParameterValidator validator = valueType.getValidator();
        if (validator instanceof MinMaxValidator) {
            MinMaxValidator minMax = (MinMaxValidator) validator;
            Long min = minMax.getMin();
            if (min != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                    case BYTES:
                        result.get(ModelDescriptionConstants.MIN_LENGTH).set(min);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MIN).set(min);
                }
            }
            Long max = minMax.getMax();
            if (max != null) {
                switch (valueType.getType()) {
                    case STRING:
                    case LIST:
                    case OBJECT:
                    case BYTES:
                        result.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MAX).set(max);
                }
            }
        }
        return result;
    }

    @Override
    protected void addAllowedValuesToDescription(ModelNode result, ParameterValidator validator) {
        //Don't add allowed values for object types, since they simply enumerate the fields given in the value type
    }

    public static final class Builder extends AbstractAttributeDefinitionBuilder<Builder, ObjectTypeAttributeDefinition> {
        private String suffix;
        private final AttributeDefinition[] valueTypes;

        public Builder(final String name, final AttributeDefinition... valueTypes) {
            super(name, ModelType.OBJECT, true);
            this.valueTypes = valueTypes;

        }

        public static Builder of(final String name, final AttributeDefinition... valueTypes) {
            return new Builder(name, valueTypes);
        }

        public static Builder of(final String name, final AttributeDefinition[] valueTypes, final AttributeDefinition[] moreValueTypes) {
            ArrayList<AttributeDefinition> list = new ArrayList<AttributeDefinition>(Arrays.asList(valueTypes));
            list.addAll(Arrays.asList(moreValueTypes));
            AttributeDefinition[] allValueTypes = new AttributeDefinition[list.size()];
            list.toArray(allValueTypes);

            return new Builder(name, allValueTypes);
        }

        public ObjectTypeAttributeDefinition build() {
            if (xmlName == null) { xmlName = name; }
            return new ObjectTypeAttributeDefinition(name, xmlName, suffix, valueTypes, allowNull, validator, corrector, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        }

        public Builder setSuffix(final String suffix) {
            this.suffix = suffix;
            return this;
        }

        /*
       --------------------------
       added for binary compatibility for running compatibilty tests
        */
        @Override
        public Builder setAllowNull(boolean allowNull) {
            return super.setAllowNull(allowNull);
        }
    }
}
