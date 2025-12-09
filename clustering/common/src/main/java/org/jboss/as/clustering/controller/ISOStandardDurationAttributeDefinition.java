/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.time.Duration;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * Attribute definition for ISO 8601 standard duration strings that resolves to a {@link Duration}.
 * This directly maps to the XSD type {@code xs:duration}.
 * Values are validated by calling {@link Duration#parse(CharSequence)}.
 *
 * @author Radoslav Husar
 */
public class ISOStandardDurationAttributeDefinition extends SimpleAttributeDefinition implements ResourceModelResolver<Duration> {

    ISOStandardDurationAttributeDefinition(Builder builder) {
        super(builder);
    }

    @Override
    public Duration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        String value = this.resolveModelAttribute(context, model).asStringOrNull();
        return (value != null) ? Duration.parse(value) : null;
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, ISOStandardDurationAttributeDefinition> {

        public Builder(String attributeName) {
            super(attributeName, ModelType.STRING);
            this.setAllowExpression(true);
            this.setFlags(Flag.RESTART_RESOURCE_SERVICES);
            this.setValidator((parameterName, value) -> {
                if (value.isDefined()) {
                    String stringValue = value.asString();
                    try {
                        Duration.parse(stringValue);
                    } catch (Exception e) {
                        throw new OperationFailedException("Invalid ISO 8601 standard duration format for " + parameterName + ": " + stringValue, e);
                    }
                }
            });
        }

        public Builder(String attributeName, ISOStandardDurationAttributeDefinition basis) {
            super(attributeName, basis);
        }

        public Builder setDefaultValue(Duration duration) {
            if (duration != null) {
                this.setRequired(false);
                this.setDefaultValue(new ModelNode(duration.toString()));
            }
            return this;
        }

        @Override
        public ISOStandardDurationAttributeDefinition build() {
            return new ISOStandardDurationAttributeDefinition(this);
        }
    }
}
