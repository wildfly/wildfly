/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ee.subsystem;

import java.util.concurrent.TimeUnit;
import org.jboss.as.controller.AbstractAttributeDefinitionBuilder;

import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.EnumValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.transform.OperationTransformer;
import org.jboss.as.controller.transform.OperationTransformer.TransformedOperation;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

import static org.jboss.as.controller.transform.OperationResultTransformer.ORIGINAL_RESULT;

/**
 *
 * @author <a href="mailto:ehugonne@redhat.com">Emmanuel Hugonnet</a> (c) 2014 Red Hat, inc.
 */
public class DurationAttributeDefinition extends SimpleAttributeDefinition {

    final SimpleAttributeDefinition DURATION_TIME;

    final SimpleAttributeDefinition DURATION_UNIT = new SimpleAttributeDefinitionBuilder(CommonAttributes.UNIT, ModelType.STRING, false)
            .setAllowExpression(true)
            .setValidator(new EnumValidator<TimeUnit>(TimeUnit.class, false, true))
            .build();

    private final DurationAttributeTransformer transformer;

    protected DurationAttributeDefinition(AbstractAttributeDefinitionBuilder<?, ? extends DurationAttributeDefinition> builder) {
        super(builder.getMeasurementUnit() == null ? builder.setMeasurementUnit(MeasurementUnit.MILLISECONDS): builder);
        SimpleAttributeDefinitionBuilder durationBuilder = new SimpleAttributeDefinitionBuilder(CommonAttributes.TIME, ModelType.LONG, builder.isAllowNull())
                .setAllowExpression(builder.isAllowExpression()).setMeasurementUnit(builder.getMeasurementUnit());
        if (builder.getDefaultValue() != null && builder.getDefaultValue().isDefined()) {
            durationBuilder.setDefaultValue(builder.getDefaultValue());
        }
        this.DURATION_TIME = durationBuilder.build();
        transformer = new DurationAttributeTransformer();
        ((DurationAttributeValidator)builder.getValidator()).setAttribute(this);
    }

    public DurationAttributeTransformer getTransformer() {
        return transformer;
    }

    private static class DurationAttributeValidator implements ParameterValidator {

        private DurationAttributeDefinition definition;

        private void setAttribute(DurationAttributeDefinition definition) {
            this.definition = definition;
        }
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if (value.isDefined()) {
                if (value.hasDefined(CommonAttributes.TIME) && value.get(CommonAttributes.TIME).getType() != ModelType.EXPRESSION) {
                    definition.DURATION_TIME.getValidator().validateParameter(CommonAttributes.TIME, value.get(CommonAttributes.TIME));
                    if (value.hasDefined(CommonAttributes.UNIT) && value.get(CommonAttributes.UNIT).getType() != ModelType.EXPRESSION) {
                        definition.DURATION_UNIT.getValidator().validateParameter(CommonAttributes.UNIT, value.get(CommonAttributes.UNIT));
                        TimeUnit unit = TimeUnit.valueOf(value.get(CommonAttributes.UNIT).asString());
                        value.set(unit.toMillis(value.get(CommonAttributes.TIME).asLong()));
                    }
                } else {
                    definition.DURATION_TIME.getValidator().validateParameter(parameterName, value);
                }
            }
        }

        @Override
        public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
            validateParameter(parameterName, value.resolve());
        }
    }

    private class DurationAttributeTransformer implements OperationTransformer {
        @Override
        public TransformedOperation transformOperation(TransformationContext context, PathAddress address, ModelNode operation) throws OperationFailedException {
            final ModelNode transformedOperation = operation.clone();
            if (operation.hasDefined(getName())) {
                ModelNode attributeNode = transformedOperation.get(getName());
                if (attributeNode.hasDefined(CommonAttributes.TIME) && attributeNode.hasDefined(CommonAttributes.UNIT)) {
                    TimeUnit unit = TimeUnit.valueOf(attributeNode.get(CommonAttributes.UNIT).asString());
                    TimeUnit targetUnit = TimeUnit.valueOf(DURATION_TIME.getMeasurementUnit().toString());
                    attributeNode.set(unit.convert(attributeNode.get(CommonAttributes.TIME).asLong(), targetUnit));
                }
            }
            return new TransformedOperation(transformedOperation, ORIGINAL_RESULT);
        }
    }

    public static final class Builder extends AbstractAttributeDefinitionBuilder<Builder, DurationAttributeDefinition> {

        public Builder(final String name, boolean allowNull) {
            super(name, ModelType.LONG, allowNull);
            setValidator(new DurationAttributeValidator());
        }

        @Override
        public DurationAttributeDefinition build() {
            return new DurationAttributeDefinition(this);
        }
    }
}
