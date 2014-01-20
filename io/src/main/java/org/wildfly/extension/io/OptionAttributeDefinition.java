/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 * /
 */

package org.wildfly.extension.io;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;

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
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.xnio.Option;
import org.xnio.OptionMap;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
public class OptionAttributeDefinition extends SimpleAttributeDefinition {
    private final Option option;
    private final Class<?> optionType;

    private OptionAttributeDefinition(String name, String xmlName, ModelNode defaultValue, ModelType type, boolean allowNull, boolean allowExpression,
                                      MeasurementUnit measurementUnit, ParameterCorrector corrector, ParameterValidator validator, boolean validateNull,
                                      String[] alternatives, String[] requires, AttributeMarshaller attributeMarshaller, boolean resourceOnly,
                                      DeprecationData deprecated, Option<?> option, Class<?> optionType, AccessConstraintDefinition[] accessConstraints,
                                      Boolean nullSignificant, AttributeAccess.Flag... flags) {
        super(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit, corrector, validator, validateNull, alternatives, requires,
                attributeMarshaller, resourceOnly, deprecated, accessConstraints, nullSignificant, flags);
        this.option = option;
        this.optionType = optionType;

    }

    public Option<?> getOption() {
        return option;
    }

    public OptionMap.Builder resolveOption(final OperationContext context, final ModelNode model, OptionMap.Builder builder) throws OperationFailedException {
        ModelNode value = resolveModelAttribute(context, model);
        if (value.isDefined()) {
            if (getType() == ModelType.INT) {
                builder.set((Option<Integer>) option, value.asInt());
            } else if (getType() == ModelType.LONG) {
                builder.set(option, value.asLong());
            } else if (getType() == ModelType.BOOLEAN) {
                builder.set(option, value.asBoolean());
            } else if (optionType.isEnum()) {
                builder.set(option, option.parseValue(value.asString(), option.getClass().getClassLoader()));
            }else if (option.getClass().getSimpleName().equals("SequenceOption")) {
                builder.setSequence(option, value.asString().split(","));
            } else if (getType() == ModelType.STRING) {
                builder.set(option, value.asString());
            } else {
                throw new OperationFailedException("Dont know how to handle: " + option + " with value: " + value);
            }
        }
        return builder;
    }

    public static Builder builder(String attributeName, Option<?> option) {
        return new Builder(attributeName, option);
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, OptionAttributeDefinition> {
        private Option<?> option;
        private Class<?> optionType;

        public Builder(String attributeName, Option<?> option) {
            this(attributeName, option, null);
        }

        public Builder(String attributeName, Option<?> option, ModelNode defaultValue) {
            super(attributeName, ModelType.UNDEFINED, true);
            this.option = option;
            this.defaultValue = defaultValue;
            setType();
        }

        @Override
        public OptionAttributeDefinition build() {
            return new OptionAttributeDefinition(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                    corrector, validator, validateNull, alternatives, requires, attributeMarshaller, resourceOnly,
                    deprecated, option, optionType, accessConstraints, nullSignficant, flags);
        }

        private void setType() {
            try {
                final Field typeField;
                if (option.getClass().getSimpleName().equals("SequenceOption")) {
                    typeField = option.getClass().getDeclaredField("elementType");
                } else {
                    typeField = option.getClass().getDeclaredField("type");
                }

                typeField.setAccessible(true);
                optionType = (Class<?>) typeField.get(option);

                if (optionType.isAssignableFrom(Integer.class)) {
                    type = ModelType.INT;
                } else if (optionType.isAssignableFrom(Long.class)) {
                    type = ModelType.LONG;
                } else if (optionType.isAssignableFrom(BigInteger.class)) {
                    type = ModelType.BIG_INTEGER;
                } else if (optionType.isAssignableFrom(Double.class)) {
                    type = ModelType.DOUBLE;
                } else if (optionType.isAssignableFrom(BigDecimal.class)) {
                    type = ModelType.BIG_DECIMAL;
                } else if (optionType.isEnum() || optionType.isAssignableFrom(String.class)) {
                    type = ModelType.STRING;
                } else if (optionType.isAssignableFrom(Boolean.class)) {
                    type = ModelType.BOOLEAN;
                } else {
                    type = ModelType.UNDEFINED;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
