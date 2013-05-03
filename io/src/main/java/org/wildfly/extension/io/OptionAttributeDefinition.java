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
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.xnio.Option;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */
class OptionAttributeDefinition extends SimpleAttributeDefinition {
    private final Option<?> option;

    private OptionAttributeDefinition(String name, String xmlName, ModelNode defaultValue, ModelType type, boolean allowNull, boolean allowExpression,
                                      MeasurementUnit measurementUnit, ParameterCorrector corrector, ParameterValidator validator, boolean validateNull,
                                      String[] alternatives, String[] requires, AttributeMarshaller attributeMarshaller, boolean resourceOnly,
                                      DeprecationData deprecated, Option<?> option, AttributeAccess.Flag... flags) {
        super(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit, corrector, validator, validateNull, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        this.option = option;
    }

    public Option<?> getOption() {
        return option;
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, OptionAttributeDefinition> {
        private Option<?> option;

        public Builder(String attributeName, Option<?> option) {
            super(attributeName, getType(option), true);
            this.option = option;
        }


        @Override
        public OptionAttributeDefinition build() {
            return new OptionAttributeDefinition(name, xmlName, defaultValue, type, allowNull, allowExpression, measurementUnit,
                    corrector, validator, validateNull, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, option, flags);
        }

        private static ModelType getType(Option<?> option) {

            try {
                Field typeField = option.getClass().getDeclaredField("type");
                typeField.setAccessible(true);
                Class<?> type = (Class<?>) typeField.get(option);


                if (type.isAssignableFrom(Integer.class)) {
                    return ModelType.INT;
                } else if (type.isAssignableFrom(Long.class)) {
                    return ModelType.LONG;

                } else if (type.isAssignableFrom(BigInteger.class)) {
                    return ModelType.BIG_INTEGER;
                } else if (type.isAssignableFrom(Double.class)) {
                    return ModelType.DOUBLE;
                } else if (type.isAssignableFrom(BigDecimal.class)) {
                    return ModelType.BIG_DECIMAL;

                } else if (type.isAssignableFrom(String.class)) {
                    return ModelType.STRING;

                } else if (type.isAssignableFrom(Boolean.class)) {
                    return ModelType.BOOLEAN;

                } else {
                    return ModelType.OBJECT;
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return ModelType.OBJECT;
        }
    }


}
