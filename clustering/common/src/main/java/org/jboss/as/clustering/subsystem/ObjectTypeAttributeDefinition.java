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

package org.jboss.as.clustering.subsystem;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import org.jboss.as.clustering.subsystem.validators.ObjectTypeValidator;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.AllowedValuesValidator;
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

    private ObjectTypeAttributeDefinition(final String name, final String xmlName, final String suffix, final AttributeDefinition[] valueTypes, final boolean allowNull, final ParameterCorrector corrector, final String[] alternatives, final String[] requires, final AttributeAccess.Flag... flags) {
        super(name, xmlName, null, ModelType.OBJECT, allowNull, false, null, corrector, new ObjectTypeValidator(allowNull, valueTypes), alternatives, requires, flags);
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
        addValueTypeDescription(result, prefix, bundle);
        return result;
    }

    @Override
    public ModelNode addOperationParameterDescription(ResourceBundle bundle, String prefix, ModelNode operationDescription) {
        final ModelNode result = super.addOperationParameterDescription(bundle, prefix, operationDescription);
        addValueTypeDescription(result, prefix, bundle);
        return result;
    }

    protected void addValueTypeDescription(final ModelNode node, final String prefix, final ResourceBundle bundle) {
        for (AttributeDefinition valueType : valueTypes) {
            // get the value type description of the attribute
            final ModelNode valueTypeDesc = getValueTypeDescription(valueType, false);
            final String p = (prefix == null || prefix.isEmpty()) ? suffix : String.format("%s.%s", prefix, suffix);
            // get the text description of the attribute
            valueTypeDesc.get(ModelDescriptionConstants.DESCRIPTION).set(valueType.getAttributeTextDescription(bundle, p));
            // set it as one of our value types, and return the value
            final ModelNode childType = node.get(ModelDescriptionConstants.VALUE_TYPE, valueType.getName()).set(valueTypeDesc);
            // if it is of type OBJECT itself (add its nested descriptions)
            if (valueType instanceof ObjectTypeAttributeDefinition) {
                ObjectTypeAttributeDefinition.class.cast(valueType).addValueTypeDescription(childType, prefix, bundle);
            }
        }
    }

    @Override
    public void marshallAsElement(final ModelNode resourceModel, final XMLStreamWriter writer) throws XMLStreamException {
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
        result.get(ModelDescriptionConstants.NILLABLE).set(isAllowNull());
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
                        result.get(ModelDescriptionConstants.MAX_LENGTH).set(max);
                        break;
                    default:
                        result.get(ModelDescriptionConstants.MAX).set(max);
                }
            }
        }
        if (validator instanceof AllowedValuesValidator) {
            AllowedValuesValidator avv = (AllowedValuesValidator) validator;
            List<ModelNode> allowed = avv.getAllowedValues();
            if (allowed != null) {
                for (ModelNode ok : allowed) {
                    result.get(ModelDescriptionConstants.ALLOWED).add(ok);
                }
            }
        }

        return result;
    }

    public static class Builder {
        private final String name;
        private String suffix;
        private final AttributeDefinition[] valueTypes;
        private ParameterCorrector corrector;
        private String xmlName;
        private boolean allowNull;
        private String[] alternatives;
        private String[] requires;
        private AttributeAccess.Flag[] flags;

        public Builder(final String name, final AttributeDefinition... valueTypes) {
            this.name = name;
            this.valueTypes = valueTypes;
            this.allowNull = true;
        }

        public static Builder of(final String name, final AttributeDefinition... valueTypes) {
            return new Builder(name, valueTypes);
        }

        public static Builder of(final String name, final AttributeDefinition[] valueTypes, final AttributeDefinition[] moreValueTypes) {

            ArrayList<AttributeDefinition> list = new ArrayList<AttributeDefinition>(Arrays.asList(valueTypes)) ;
            list.addAll(Arrays.asList(moreValueTypes));
            AttributeDefinition[] allValueTypes = new AttributeDefinition[list.size()];
            list.toArray(allValueTypes);

            return new Builder(name, allValueTypes);
        }

        public ObjectTypeAttributeDefinition build() {
            if (xmlName == null) xmlName = name;
            return new ObjectTypeAttributeDefinition(name, xmlName, suffix, valueTypes, allowNull, corrector, alternatives, requires, flags);
        }

        public Builder setAllowNull(final boolean allowNull) {
            this.allowNull = allowNull;
            return this;
        }

        public Builder setAlternates(final String... alternates) {
            this.alternatives = alternates;
            return this;
        }

        public Builder setCorrector(final ParameterCorrector corrector) {
            this.corrector = corrector;
            return this;
        }

        public Builder setFlags(final AttributeAccess.Flag... flags) {
            this.flags = flags;
            return this;
        }

        public Builder setRequires(final String... requires) {
            this.requires = requires;
            return this;
        }

        public Builder setSuffix(final String suffix) {
            this.suffix = suffix;
            return this;
        }

        public Builder setXmlName(final String xmlName) {
            this.xmlName = xmlName;
            return this;
        }
    }
}