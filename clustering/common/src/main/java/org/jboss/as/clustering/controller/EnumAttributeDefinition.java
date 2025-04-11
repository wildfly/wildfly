/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.operations.validation.ChainedParameterValidator;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * @author Paul Ferraro
 */
public class EnumAttributeDefinition<E extends Enum<E>> extends SimpleAttributeDefinition implements ResourceModelResolver<E> {
    private final Class<E> type;
    private final Function<String, E> resolver;

    EnumAttributeDefinition(Builder<E> builder) {
        super(builder);
        this.type = builder.type;
        this.resolver = builder.resolver;
    }

    @Override
    public E resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String value = this.resolveModelAttribute(context, model).asStringOrNull();
        return (value != null) ? this.resolver.apply(value) : null;
    }

    public static class Builder<E extends Enum<E>> extends AbstractAttributeDefinitionBuilder<Builder<E>, EnumAttributeDefinition<E>> {
        private final Class<E> type;
        private Function<String, E> resolver;

        public Builder(String attributeName, E defaultValue) {
            this(attributeName, defaultValue.getDeclaringClass());
            this.setDefaultValue(defaultValue);
        }

        public Builder(String attributeName, Class<E> type) {
            super(attributeName, ModelType.STRING);
            this.type = type;
            this.resolver = new Function<>() {
                @Override
                public E apply(String value) {
                    return Enum.valueOf(type, value);
                }
            };
            this.setAllowExpression(true);
            this.setAttributeParser(AttributeParser.SIMPLE);
            this.setFlags(Flag.RESTART_RESOURCE_SERVICES);
            this.setAllowedValues(EnumSet.allOf(type));
        }

        public Builder(EnumAttributeDefinition<E> basis) {
            this(basis.getName(), basis);
        }

        public Builder(String attributeName, EnumAttributeDefinition<E> basis) {
            super(attributeName, basis);
            this.type = basis.type;
            this.resolver = basis.resolver;
        }

        public Builder<E> setDefaultValue(E defaultValue) {
            if (defaultValue != null) {
                this.setRequired(false);
                this.setDefaultValue(new ModelNode(defaultValue.name()));
            }
            return this;
        }

        public Builder<E> setAllowedValues(Set<E> values) {
            return super.setValidator(EnumValidator.create(this.type, EnumSet.copyOf(values)));
        }

        public Builder<E> withResolver(Function<String, E> resolver) {
            this.resolver = resolver;
            return this;
        }

        @Override
        public Builder<E> setValidator(ParameterValidator validator) {
            return super.setValidator(new ChainedParameterValidator(this.getValidator(), validator));
        }

        @Override
        public EnumAttributeDefinition<E> build() {
            return new EnumAttributeDefinition<>(this);
        }
    }
}
