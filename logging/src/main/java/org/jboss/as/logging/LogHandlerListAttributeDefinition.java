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

package org.jboss.as.logging;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.DeprecationData;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.logging.resolvers.HandlerResolver;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logmanager.config.PropertyConfigurable;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LogHandlerListAttributeDefinition extends SimpleListAttributeDefinition implements ConfigurationProperty<Set<String>> {
    private final String propertyName;
    private final HandlerResolver resolver = HandlerResolver.INSTANCE;

    LogHandlerListAttributeDefinition(final String name, final String xmlName, final String propertyName, final AttributeDefinition valueType,
                                      final boolean allowNull, final int minSize, final int maxSize, final String[] alternatives, final String[] requires,
                                      final AttributeMarshaller attributeMarshaller, final boolean resourceOnly,final DeprecationData deprecationData,
                                      final AttributeAccess.Flag... flags) {
        super(name, xmlName, valueType,allowNull, minSize, maxSize,  alternatives, requires, attributeMarshaller, resourceOnly, deprecationData, flags);
        this.propertyName = propertyName;
    }

    @Override
    public ModelNodeResolver<Set<String>> resolver() {
        return resolver;
    }

    @Override
    public String getPropertyName() {
        return propertyName;
    }

    @Override
    public Set<String> resolvePropertyValue(final OperationContext context, final ModelNode model) throws OperationFailedException {
        Set<String> result = Collections.emptySet();
        final ModelNode value = resolveModelAttribute(context, model);
        if (value.isDefined()) {
            result = resolver.resolveValue(context, value);
        }
        return result;
    }

    @Override
    public void setPropertyValue(final OperationContext context, final ModelNode model, final PropertyConfigurable configuration) throws OperationFailedException {
        throw LoggingMessages.MESSAGES.unsupportedMethod("setPropertyValue", getClass().getName());
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, LogHandlerListAttributeDefinition> {

        private String propertyName;


        Builder(final String name) {
            super(name, ModelType.LIST);
        }

        /**
         * Creates a builder for {@link LogHandlerListAttributeDefinition}.
         *
         * @param name      the name of the attribute
         *
         * @return the builder
         */
        public static Builder of(final String name) {
            return new Builder(name);
        }

        public LogHandlerListAttributeDefinition build() {
            if (xmlName == null) xmlName = name;
            if (maxSize < 1) maxSize = Integer.MAX_VALUE;
            if (propertyName == null) propertyName = name;
            if (attributeMarshaller == null) attributeMarshaller = HandlersAttributeMarshaller.INSTANCE;
            return new LogHandlerListAttributeDefinition(name, xmlName, propertyName, CommonAttributes.HANDLER, allowNull, minSize, maxSize, alternatives, requires, attributeMarshaller, resourceOnly, deprecated, flags);
        }

        public Builder setPropertyName(final String propertyName) {
            this.propertyName = propertyName;
            return this;
        }
    }
}
