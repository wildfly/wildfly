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

import org.jboss.as.controller.ListAttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleListAttributeDefinition;
import org.jboss.as.logging.resolvers.HandlerResolver;
import org.jboss.as.logging.resolvers.ModelNodeResolver;
import org.jboss.dmr.ModelNode;
import org.jboss.logmanager.config.PropertyConfigurable;

/**
 * Date: 13.10.2011
 *
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class LogHandlerListAttributeDefinition extends SimpleListAttributeDefinition implements ConfigurationProperty<Set<String>> {
    private final String propertyName;
    private final HandlerResolver resolver = HandlerResolver.INSTANCE;

    private LogHandlerListAttributeDefinition(Builder builder) {
        super(builder, CommonAttributes.HANDLER);
        this.propertyName = builder.propertyName;
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

    public static class Builder extends ListAttributeDefinition.Builder<Builder, LogHandlerListAttributeDefinition> {

        private String propertyName;


        Builder(final String name) {
            super(name);
            setElementValidator(CommonAttributes.HANDLER.getValidator());
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
            if (propertyName == null) propertyName = name;
            if (attributeMarshaller == null) attributeMarshaller = HandlersAttributeMarshaller.INSTANCE;
            return new LogHandlerListAttributeDefinition(this);
        }

        public Builder setPropertyName(final String propertyName) {
            this.propertyName = propertyName;
            return this;
        }
    }
}
