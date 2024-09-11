/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.clustering.controller.validation;

import java.util.function.DoubleSupplier;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 *
 */
public class DoubleRangeValidator extends ModelTypeValidator {
    public static final ParameterValidator NON_NEGATIVE = new DoubleRangeValidator(Bound.inclusive(0d));
    public static final ParameterValidator POSITIVE = new DoubleRangeValidator(Bound.exclusive(0d));
    public static final ParameterValidator FLOAT = new DoubleRangeValidator(Bound.inclusive(0f - Float.MAX_VALUE), Bound.inclusive(Float.MAX_VALUE));
    public static final ParameterValidator NON_NEGATIVE_FLOAT = new DoubleRangeValidator(Bound.inclusive(0f), Bound.inclusive(Float.MAX_VALUE));
    public static final ParameterValidator POSITIVE_FLOAT = new DoubleRangeValidator(Bound.inclusive(Float.MIN_VALUE), Bound.inclusive(Float.MAX_VALUE));

    public interface Bound extends DoubleSupplier {
        enum Type {
            INCLUSIVE, EXCLUSIVE;
        }

        Type getType();

        static Bound inclusive(double value) {
            return new Bound() {
                @Override
                public double getAsDouble() {
                    return value;
                }

                @Override
                public Type getType() {
                    return Type.INCLUSIVE;
                }
            };
        }

        static Bound exclusive(double value) {
            return new Bound() {
                @Override
                public double getAsDouble() {
                    return value;
                }

                @Override
                public Type getType() {
                    return Type.EXCLUSIVE;
                }
            };
        }
    }

    private final Bound lowerBound;
    private final Bound upperBound;

    /**
     * Creates an upper- and lower-bounded validator.
     * @param lowerBound the lower bound
     */
    DoubleRangeValidator(Bound lowerBound) {
        this(lowerBound, null);
    }

    /**
     * Creates an upper- and lower-bounded validator.
     * @param lowerBound the lower bound, or null if no lower bound
     * @param upperBound the upper bound, or null if no upper bound
     */
    DoubleRangeValidator(Bound lowerBound, Bound upperBound) {
        super(ModelType.DOUBLE);
        this.lowerBound = lowerBound;
        this.upperBound = upperBound;
    }

    @Override
    public void validateParameter(String parameterName, ModelNode parameterValue) throws OperationFailedException {
        super.validateParameter(parameterName, parameterValue);
        if (parameterValue.isDefined() && parameterValue.getType() != ModelType.EXPRESSION) {
            double value = parameterValue.asDouble();
            if (this.lowerBound != null) {
                double bound = this.lowerBound.getAsDouble();
                boolean exclusive = this.lowerBound.getType() == Bound.Type.EXCLUSIVE;
                if ((value < bound) || (exclusive && (value == bound))) {
                    throw ClusteringLogger.ROOT_LOGGER.parameterValueOutOfBounds(parameterName, value, exclusive ? ">" : ">=", bound);
                }
            }
            if (this.upperBound != null) {
                double bound = this.upperBound.getAsDouble();
                boolean exclusive = this.upperBound.getType() == Bound.Type.EXCLUSIVE;
                if ((value > bound) || (exclusive && (value == bound))) {
                    throw ClusteringLogger.ROOT_LOGGER.parameterValueOutOfBounds(parameterName, value, exclusive ? "<" : "<=", bound);
                }
            }
        }
    }
}
