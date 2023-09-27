/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.singleton;

/**
 * AccountManager
 *
 * @author Jaikiran Pai
 */
public interface AccountManager {

    /**
     * Credits the amount to the account
     *
     * @param amount Amount to be credited
     * @return
     */
    void credit(int amount);

    /**
     * Debits the amount from the account
     *
     * @param amount Amount to be debited
     * @return
     */
    void debit(int amount);

    int balance();
}
