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
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.ParameterCorrector;
import org.jboss.as.controller.operations.validation.ObjectTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.config.PropertyConfigurable;

/**
 * Date: 15.11.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class PropertyObjectTypeAttributeDefinition extends ObjectTypeAttributeDefinition implements ConfigurationProperty<String> {
    private final ModelNodeResolver<String> resolver;
    private final String propertyName;

    private PropertyObjectTypeAttributeDefinition(final String name, final String xmlName, final String propertyName, final String suffix,
                                                  final AttributeDefinition[] valueTypes, final boolean allowNull, final ModelNodeResolver<String> resolver,
                                                  final ParameterValidator validator, final ParameterCorrector corrector, final String[] alternatives, final String[] requires,
                                                  final AttributeMarshaller attributeMarshaller, final boolean resourceOnly, final DeprecationData deprecationData,
                                                  final AttributeAccess.Flag... flags) {
        super(name, xmlName, suffix, valueTypes, allowNull, validator, corrector, alternatives, requires, attributeMarshaller, resourceOnly, deprecationData, flags);
        this.propertyName = propertyName;
        this.resolver = resolver;
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
    public void setPropertyValue(final OperationContext context, final ModelNode model, final PropertyConfigurable configuration) throws OperationFailedException {
        final String value = resolvePropertyValue(context, model);
        if (value == null) {
            configuration.removeProperty(propertyName);
        } else {
            configuration.setPropertyValueString(propertyName, value);
        }
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, PropertyObjectTypeAttributeDefinition> {
        private ModelNodeResolver<String> resolver;
        private String propertyName;
        private String suffix;
        private final AttributeDefinition[] valueTypes;

        public Builder(final String name, final AttributeDefinition... valueTypes) {
            super(name, ModelType.OBJECT, true);
            this.valueTypes = valueTypes;
        }

        public static Builder of(final String name, final AttributeDefinition... valueTypes) {
            return new Builder(name, valueTypes);
        }

        public PropertyObjectTypeAttributeDefinition build() {
            if (xmlName == null) xmlName = name;
            ParameterValidator validator = this.validator;
            if (validator == null) {
                validator = new ObjectTypeValidator(allowNull, valueTypes);
            }
            return new PropertyObjectTypeAttributeDefinition(name, xmlName, propertyName, suffix, valueTypes, allowNull, resolver, validator, corrector, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        }

        public Builder setSuffix(final String suffix) {
            this.suffix = suffix;
            return this;
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