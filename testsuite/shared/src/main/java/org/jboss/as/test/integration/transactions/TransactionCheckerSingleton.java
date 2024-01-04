/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.transactions;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.ejb.LocalBean;
import jakarta.ejb.Singleton;

/**
 * Singleton class used as log for actions done during testing.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@Singleton
@LocalBean
public class TransactionCheckerSingleton implements TransactionCheckerSingletonRemote {
    private final AtomicInteger committed = new AtomicInteger();
    private final AtomicInteger prepared = new AtomicInteger();
    private final AtomicInteger rolledback = new AtomicInteger();
    private final AtomicInteger synchronizedBegin = new AtomicInteger();
    private final AtomicInteger synchronizedBefore = new AtomicInteger();
    private final AtomicInteger synchronizedAfter = new AtomicInteger();
    private final AtomicInteger synchronizedAfterCommitted = new AtomicInteger();
    private final AtomicInteger synchronizedAfterRolledBack = new AtomicInteger();
    private final Map<String,String> messages = new ConcurrentHashMap<>();

    @Override
    public int getCommitted() {
        return committed.get();
    }

    @Override
    public void addCommit() {
        committed.incrementAndGet();
    }

    @Override
    public int getPrepared() {
        return prepared.get();
    }

    @Override
    public void addPrepare() {
        prepared.incrementAndGet();
    }

    @Override
    public int getRolledback() {
        return rolledback.get();
    }

    @Override
    public void addRollback() {
        rolledback.incrementAndGet();
    }

    @Override
    public boolean isSynchronizedBefore() {
        return synchronizedBefore.get() > 0;
    }

    @Override
    public void setSynchronizedBefore() {
        synchronizedBefore.incrementAndGet();
    }

    @Override
    public boolean isSynchronizedAfter() {
        return synchronizedAfter.get() > 0;
    }

    @Override
    public void setSynchronizedAfter(boolean isCommit) {
        synchronizedAfter.incrementAndGet();
        if(isCommit) {
            synchronizedAfterCommitted.incrementAndGet();
        } else {
            synchronizedAfterRolledBack.incrementAndGet();
        }
    }

    @Override
    public boolean isSynchronizedBegin() {
        return synchronizedBegin.get() > 0;
    }

    @Override
    public void setSynchronizedBegin() {
        synchronizedBegin.incrementAndGet();
    }

    @Override
    public void resetCommitted() {
        committed.set(0);
    }

    @Override
    public void resetPrepared() {
        prepared.set(0);
    }

    @Override
    public void resetRolledback() {
        rolledback.set(0);
    }

    @Override
    public void resetSynchronizedBefore() {
        synchronizedBefore.set(0);
    }

    @Override
    public void resetSynchronizedAfter() {
        synchronizedAfter.set(0);
        synchronizedAfterCommitted.set(0);
        synchronizedAfterRolledBack.set(0);
    }

    @Override
    public void resetSynchronizedBegin() {
        synchronizedBegin.set(0);
    }

    @Override
    public int countSynchronizedBefore() {
        return synchronizedBefore.get();
    }

    @Override
    public int countSynchronizedAfter() {
        return synchronizedAfter.get();
    }

    @Override
    public int countSynchronizedAfterCommitted() {
        return synchronizedAfterCommitted.get();
    }

    @Override
    public int countSynchronizedAfterRolledBack() {
        return synchronizedAfterRolledBack.get();
    }

    @Override
    public int countSynchronizedBegin() {
        return synchronizedBegin.get();
    }

    @Override
    public void addMessage(String msg) {
        messages.put(msg,msg);
    }

    @Override
    public Collection<String> getMessages() {
        return messages.values();
    }

    @Override
    public void resetMessages() {
        messages.clear();
    }

    @Override
    public void resetAll() {
        resetCommitted();
        resetPrepared();
        resetRolledback();
        resetSynchronizedAfter();
        resetSynchronizedBefore();
        resetSynchronizedBegin();
        resetMessages();
    }
}
