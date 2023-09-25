/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.beanvalidation.hibernate.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.validator.spi.group.DefaultGroupSequenceProvider;

/**
 * @author Madhumita Sadhukhan
 */
public class CarGroupSequenceProvider implements DefaultGroupSequenceProvider<Car> {

    private static final List<Class<?>> DEFAULT_SEQUENCE = Arrays.asList(Car.class, CarChecks.class);

    @Override
    public List<Class<?>> getValidationGroups(Car car) {
        if (car == null || !car.inspectionCompleted()) {
            return DEFAULT_SEQUENCE;
        } else {
            List<Class<?>> finalInspectionSequence = new ArrayList<>(3);
            finalInspectionSequence.addAll(DEFAULT_SEQUENCE);
            finalInspectionSequence.add(FinalInspection.class);

            return finalInspectionSequence;
        }
    }
}
