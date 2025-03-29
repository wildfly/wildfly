/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess.Flag;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.wildfly.subsystem.resource.ResourceModelResolver;

/**
 * Attribute definition of a given unit that resolves to a time duration.
 * @author Paul Ferraro
 */
public class DurationAttributeDefinition extends SimpleAttributeDefinition implements ResourceModelResolver<Duration> {
    private final ChronoUnit unit;

    DurationAttributeDefinition(Builder builder) {
        super(builder);
        this.unit = builder.unit;
    }

    @Override
    public Duration resolve(OperationContext context, ModelNode model) throws OperationFailedException {
        Long value = this.resolveModelAttribute(context, model).asLongOrNull();
        return (value != null) ? Duration.of(value, this.unit) : null;
    }

    public static class Builder extends AbstractAttributeDefinitionBuilder<Builder, DurationAttributeDefinition> {
        private final ChronoUnit unit;

        public Builder(String attributeName, ChronoUnit unit) {
            super(attributeName, ModelType.LONG);
            this.unit = unit;
            this.setAllowExpression(true);
            this.setFlags(Flag.RESTART_RESOURCE_SERVICES);
            this.setMeasurementUnit(toMeasurementUnit(unit));
            this.setValidator(new LongRangeValidator(0));
        }

        public Builder(String attributeName, DurationAttributeDefinition basis) {
            super(attributeName, basis);
            this.unit = basis.unit;
        }

        public Builder setDefaultValue(Duration duration) {
            if (duration != null) {
                this.setRequired(false);
                this.setDefaultValue(duration.isZero() ? ModelNode.ZERO_LONG : new ModelNode(this.unit.between(Instant.EPOCH, Instant.EPOCH.plus(duration))));
            }
            return this;
        }

        @Override
        public DurationAttributeDefinition build() {
            return new DurationAttributeDefinition(this);
        }
    }

    static MeasurementUnit toMeasurementUnit(ChronoUnit unit) {
        switch (unit) {
            case NANOS:
                return MeasurementUnit.NANOSECONDS;
            case MICROS:
                return MeasurementUnit.MICROSECONDS;
            case MILLIS:
                return MeasurementUnit.MILLISECONDS;
            case SECONDS:
                return MeasurementUnit.SECONDS;
            case MINUTES:
                return MeasurementUnit.MINUTES;
            case HOURS:
                return MeasurementUnit.HOURS;
            case DAYS:
                return MeasurementUnit.DAYS;
            default:
                throw new IllegalArgumentException(unit.name());
        }
    }
}
