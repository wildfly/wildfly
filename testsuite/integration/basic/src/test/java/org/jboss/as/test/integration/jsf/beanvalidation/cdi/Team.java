/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.jsf.beanvalidation.cdi;

import java.io.Serializable;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Named;
import jakarta.validation.constraints.Size;

/**
 * A Team.
 *
 * @author Farah Juma
 */
@Named("team")
@SessionScoped
public class Team implements Serializable {

    @CustomMin
    private int numberOfPeople;

    @Size(min = 3, message = "Team name must be at least 3 characters.")
    private String name;

    public Team() {
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getNumberOfPeople() {
        return this.numberOfPeople;
    }

    public void setNumberOfPeople(int numberOfPeople) {
        this.numberOfPeople = numberOfPeople;
    }
}

