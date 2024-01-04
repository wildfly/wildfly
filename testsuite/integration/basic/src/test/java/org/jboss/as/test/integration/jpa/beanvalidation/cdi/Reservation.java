/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jpa.beanvalidation.cdi;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;

/**
 * Reservation entity class.
 *
 * @author Farah Juma
 */
@Entity
public class Reservation {

    @Id
    @GeneratedValue
    private int id;

    @CustomMin
    private int numberOfPeople;

    @NotNull(message = "may not be null")
    private String lastName;

    public Reservation(int numberOfPeople, String lastName) {
        this.numberOfPeople = numberOfPeople;
        this.lastName = lastName;
    }
}
