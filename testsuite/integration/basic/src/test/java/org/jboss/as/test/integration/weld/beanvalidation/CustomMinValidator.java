/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.weld.beanvalidation;

import javax.inject.Inject;
import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * A validator that uses constructor injection.
 *
 * @author Farah Juma
 */
public class CustomMinValidator implements ConstraintValidator<CustomMin, Integer> {

    private final MinimumValueProvider minimumValueProvider;

    @Inject
    public CustomMinValidator(MinimumValueProvider minimumValueProvider) {
        this.minimumValueProvider = minimumValueProvider;
    }

    @Override
    public void initialize(CustomMin constraintAnnotation) {
    }

    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        return value >= minimumValueProvider.getMin();
    }
}
