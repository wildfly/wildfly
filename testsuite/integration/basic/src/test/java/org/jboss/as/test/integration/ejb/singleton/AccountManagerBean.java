/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton;

import jakarta.ejb.EJB;
import jakarta.ejb.LocalBean;
import jakarta.ejb.Remote;
import jakarta.ejb.Singleton;

/**
 * AccountManagerBean
 *
 * @author Jaikiran Pai
 */
@Singleton
@LocalBean
@Remote(AccountManager.class)
public class AccountManagerBean implements AccountManager {
    /**
     * Inject the no-interface view of the Calculator
     */
    @EJB
    private Calculator simpleCalculator;

    private int balance;

    /**
     * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#credit(int)
     */
    public void credit(int amount) {
        this.balance = this.simpleCalculator.add(this.balance, amount);

    }

    /**
     * @see org.jboss.ejb3.nointerface.integration.test.common.AccountManager#debit(int)
     */
    public void debit(int amount) {
        this.balance = this.simpleCalculator.subtract(this.balance, amount);
    }

    public int balance() {
        return this.balance;
    }

    public void throwException() {
        throw new IllegalArgumentException();
    }

}
