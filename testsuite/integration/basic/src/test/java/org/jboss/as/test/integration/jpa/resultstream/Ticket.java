/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.jpa.resultstream;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Ticket entity class
 *
 * @author Zbyněk Roubalík
 */
@Entity
public class Ticket {

    Long id;
    String number;

    public Ticket() {
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long long1) {
        id = long1;
    }

    public String getNumber() {
        return number;
    }

    public void setNumber(String string) {
        number = string;
    }
}

