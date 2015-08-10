/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015, Red Hat, Inc., and individual contributors
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
