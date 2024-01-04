/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions;

import jakarta.transaction.Synchronization;
import org.jboss.logging.Logger;

/**
 * Transaction {@link Synchronization} class for testing purposes.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
public class TestSynchronization implements Synchronization {
    private static final Logger log = Logger.getLogger(TestSynchronization.class);

    private TransactionCheckerSingletonRemote checker;

    public TestSynchronization(TransactionCheckerSingletonRemote checker) {
        this.checker = checker;
    }

    @Override
    public void beforeCompletion() {
        log.tracef("beforeCompletion called");
        checker.setSynchronizedBefore();
    }

    /**
     * For status see {@link jakarta.transaction.Status}.
     */
    @Override
    public void afterCompletion(int status) {
        log.tracef("afterCompletion called with status '%s'", status);
        boolean isCommitted = status == jakarta.transaction.Status.STATUS_COMMITTED ? true : false;
        checker.setSynchronizedAfter(isCommitted);
    }

}
