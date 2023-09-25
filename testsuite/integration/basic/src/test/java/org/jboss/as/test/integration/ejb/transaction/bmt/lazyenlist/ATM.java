/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ejb.transaction.bmt.lazyenlist;

import jakarta.ejb.Remote;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
@Remote
public interface ATM {
    long createAccount(double balance);

    double getBalance(long id);

    double depositTwice(long id, double a1, double a2);

    double depositTwiceRawSQL(long id, double a1, double a2);

    double depositTwiceWithRollback(long id, double a1, double a2);

    double withdrawTwiceWithRollback(long id, double a1, double a2);
}
