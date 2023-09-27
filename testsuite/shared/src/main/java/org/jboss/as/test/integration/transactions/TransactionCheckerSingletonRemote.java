/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions;

import java.util.Collection;

import jakarta.ejb.Remote;

/**
 * Interface used as remote point to {@link TransactionCheckerSingleton} class
 * that is used for verification of test workflow.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@Remote
public interface TransactionCheckerSingletonRemote {
    int getCommitted();
    void addCommit();
    void resetCommitted();
    int getPrepared();
    void addPrepare();
    void resetPrepared();
    int getRolledback();
    void addRollback();
    void resetRolledback();
    boolean isSynchronizedBefore();
    int countSynchronizedBefore();
    void setSynchronizedBefore();
    void resetSynchronizedBefore();
    boolean isSynchronizedAfter();
    int countSynchronizedAfter();
    int countSynchronizedAfterCommitted();
    int countSynchronizedAfterRolledBack();
    void setSynchronizedAfter(boolean isCommit);
    void resetSynchronizedAfter();
    boolean isSynchronizedBegin();
    int countSynchronizedBegin();
    void setSynchronizedBegin();
    void resetSynchronizedBegin();
    void addMessage(String msg);
    Collection<String> getMessages();
    void resetMessages();
    void resetAll();
}
