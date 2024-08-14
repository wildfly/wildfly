/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * A standard {@value ModelDescriptionConstants#STATISTICS_ENABLED} attribute definition.
 * @author Paul Ferraro
 */
public class StatisticsEnabledAttributeDefinition extends SimpleAttributeDefinition implements ResourceModelResolver<Boolean> {

    StatisticsEnabledAttributeDefinition(Builder builder) {
        super(builder);
    }

    @Override
    public Boolean resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        return this.resolveModelAttribute(context, model).asBooleanOrNull();
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, StatisticsEnabledAttributeDefinition> {

        public Builder() {
            this(ModelDescriptionConstants.STATISTICS_ENABLED);
        }

        public Builder(String attributeName) {
            super(attributeName, ModelType.BOOLEAN);
            this.setAllowExpression(true);
            this.setRequired(false);
            this.setDefaultValue(ModelNode.FALSE);
            this.setAttributeParser(AttributeParser.SIMPLE);
            this.setFlags(AttributeAccess.Flag.RESTART_RESOURCE_SERVICES);
        }

        public Builder(String attributeName, ModuleAttributeDefinition basis) {
            super(attributeName, basis);
        }

        @Override
        public StatisticsEnabledAttributeDefinition build() {
            return new StatisticsEnabledAttributeDefinition(this);
        }
    }
}
