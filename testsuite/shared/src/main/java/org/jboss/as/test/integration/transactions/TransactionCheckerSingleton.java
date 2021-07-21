/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.test.integration.transactions;

import javax.ejb.LocalBean;
import javax.ejb.Singleton;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

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
