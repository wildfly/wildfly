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
package org.jboss.as.clustering.controller.validation;

import org.jboss.as.clustering.logging.ClusteringLogger;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.operations.validation.ModelTypeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * A builder for creating a range validator for {@link ModelType#DOUBLE} parameters.
 * @author Paul Ferraro
 */
public class DoubleRangeValidatorBuilder extends AbstractParameterValidatorBuilder {
    private Bound upperBound;
    private Bound lowerBound;

    /**
     * Sets an inclusive lower bound of this validator.
     * @param value the lower bound
     */
    public DoubleRangeValidatorBuilder lowerBound(double value) {
        this.lowerBound = new Bound(value, false);
        return this;
    }

    /**
     * Sets an exclusive lower bound of this validator.
     * @param value the lower bound
     */
    public DoubleRangeValidatorBuilder lowerBoundExclusive(double value) {
        this.lowerBound = new Bound(value, true);
        return this;
    }

    /**
     * Sets the inclusive upper bound of this validator.
     * @param value the upper bound
     */
    public DoubleRangeValidatorBuilder upperBound(double value) {
        this.upperBound = new Bound(value, false);
        return this;
    }

    /**
     * Sets the exclusive upper bound of this validator.
     * @param value the upper bound
     */
    public DoubleRangeValidatorBuilder upperBoundExclusive(double value) {
        this.upperBound = new Bound(value, true);
        return this;
    }

    @Override
    public ParameterValidator build() {
        return new DoubleRangeValidator(this.lowerBound, this.upperBound, this.allowsUndefined, this.allowsExpressions);
    }

    private static class Bound {
        private final double value;
        private final boolean exclusive;

        Bound(double value, boolean exclusive) {
            this.value = value;
            this.exclusive = exclusive;
        }

        double getValue() {
            return this.value;
        }

        boolean isExclusive() {
            return this.exclusive;
        }
    }

    private static class DoubleRangeValidator extends ModelTypeValidator {
        private final Bound lowerBound;
        private final Bound upperBound;

        /**
         * Creates an upper- and lower-bounded validator.
         * @param lowerBound the lower bound
         * @param upperBound the upper bound
         * @param nullable indicates whether {@link ModelType#UNDEFINED} is allowed
         * @param allowExpressions whether {@link ModelType#EXPRESSION} is allowed
         */
        DoubleRangeValidator(Bound lowerBound, Bound upperBound, boolean nullable, boolean allowExpressions) {
            super(ModelType.DOUBLE, nullable, allowExpressions, false);
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        public void validateParameter(String parameterName, ModelNode parameterValue) throws OperationFailedException {
            super.validateParameter(parameterName, parameterValue);
            if (parameterValue.isDefined() && parameterValue.getType() != ModelType.EXPRESSION) {
                double value = parameterValue.asDouble();
                if (this.lowerBound != null) {
                    double bound = this.lowerBound.getValue();
                    boolean exclusive = this.lowerBound.isExclusive();
                    if ((value < bound) || (exclusive && (value == bound))) {
                        throw ClusteringLogger.ROOT_LOGGER.parameterValueOutOfBounds(parameterName, value, exclusive ? ">" : ">=", bound);
                    }
                }
                if (this.upperBound != null) {
                    double bound = this.upperBound.getValue();
                    boolean exclusive = this.upperBound.isExclusive();
                    if ((value > bound) || (exclusive && (value == bound))) {
                        throw ClusteringLogger.ROOT_LOGGER.parameterValueOutOfBounds(parameterName, value, exclusive ? "<" : "<=", bound);
                    }
                }
            }
        }
    }
}
