/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.controller.validation;

import org.jboss.as.controller.operations.validation.LongRangeValidator;
import org.jboss.as.controller.operations.validation.ParameterValidator;

/**
 * @author Paul Ferraro
 */
public class LongRangeValidatorBuilder extends AbstractParameterValidatorBuilder {

    private volatile long min = Long.MIN_VALUE;
    private volatile long max = Long.MAX_VALUE;

    public LongRangeValidatorBuilder min(long min) {
        this.min = min;
        return this;
    }

    public LongRangeValidatorBuilder max(long max) {
        this.max = max;
        return this;
    }

    @Override
    public ParameterValidator build() {
        return new LongRangeValidator(this.min, this.max, this.allowsUndefined, this.allowsExpressions);
    }
}
