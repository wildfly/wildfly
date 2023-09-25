/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.txbridge.fromjta.service;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.io.Serializable;

/**
 * Entity to verify the transaction participant
 * is handled correctly when transaction is bridged
 * from Jakarta Transactions to WS-AT.
 */
@Entity
public class FirstCounterEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private int counter;

    public FirstCounterEntity() {
    }

    public FirstCounterEntity(int id, int initialCounterValue) {
        this.id = id;
        this.counter = initialCounterValue;
    }

    @Id
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public void incrementCounter(int howMany) {
        setCounter(getCounter() + howMany);
    }
}
