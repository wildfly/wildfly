/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.validation;

import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;

/**
 * @author Paul Ferraro
 */
public class IntRangeValidatorBuilder extends AbstractParameterValidatorBuilder {

    private volatile int min = Integer.MIN_VALUE;
    private volatile int max = Integer.MAX_VALUE;

    public IntRangeValidatorBuilder min(int min) {
        this.min = min;
        return this;
    }

    public IntRangeValidatorBuilder max(int max) {
        this.max = max;
        return this;
    }

    @Override
    public ParameterValidator build() {
        return new IntRangeValidator(this.min, this.max, this.allowsUndefined, this.allowsExpressions);
    }
}
