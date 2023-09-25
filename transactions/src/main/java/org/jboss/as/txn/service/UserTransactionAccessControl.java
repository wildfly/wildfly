/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.txn.service;

/**
 * Objects which control access to User Transaction.
 *
 * @author Eduardo Martins
 *
 */
public interface UserTransactionAccessControl {

    /**
     * Authorizes access of user transaction.
     */
    void authorizeAccess();

}
