/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.bmt.lazyenlist;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Entity
public class Account {
    private long id;
    private double balance; // don't look, not financially save

    public double getBalance() {
        return balance;
    }

    @Id
    @GeneratedValue
    public long getId() {
        return id;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public void setId(long id) {
        this.id = id;
    }
}
