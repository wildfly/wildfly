/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.weld.beanvalidation;

import jakarta.validation.constraints.NotNull;

/**
 * A Reservation.
 *
 * @author Farah Juma
 */
public class Reservation {

    @CustomMin
    private int numberOfPeople;

    @NotNull(message = "may not be null")
    private String lastName;

    public Reservation(int numberOfPeople, String lastName) {
        this.numberOfPeople = numberOfPeople;
        this.lastName = lastName;
    }
}
