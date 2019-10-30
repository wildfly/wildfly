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

import java.util.ArrayList;
import java.util.Collection;

import javax.annotation.ManagedBean;
import javax.ejb.LocalBean;
import javax.ejb.Remote;
import javax.ejb.Singleton;

/**
 * Singleton class used as log for actions done during testing.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
@Singleton
@LocalBean
@Remote
@ManagedBean
public class TransactionCheckerSingleton implements TransactionCheckerSingletonRemote {
    private int committed, prepared, rolledback;
    private int  synchronizedBegin, synchronizedBefore, synchronizedAfter,
        synchronizedAfterCommitted, synchronizedAfterRolledBack;
    private Collection<String> messages = new ArrayList<>();

    @Override
    public int getCommitted() {
        return committed;
    }

    @Override
    public void addCommit() {
        committed++;
    }

    @Override
    public int getPrepared() {
        return prepared;
    }

    @Override
    public void addPrepare() {
        prepared++;
    }

    @Override
    public int getRolledback() {
        return rolledback;
    }

    @Override
    public void addRollback() {
        rolledback++;
    }

    @Override
    public boolean isSynchronizedBefore() {
        return synchronizedBefore > 0;
    }

    @Override
    public void setSynchronizedBefore() {
        synchronizedBefore++;
    }

    @Override
    public boolean isSynchronizedAfter() {
        return synchronizedAfter > 0;
    }

    @Override
    public void setSynchronizedAfter(boolean isCommit) {
        synchronizedAfter++;
        if(isCommit) {
            synchronizedAfterCommitted++;
        } else {
            synchronizedAfterRolledBack++;
        }
    }

    @Override
    public boolean isSynchronizedBegin() {
        return synchronizedBegin > 0;
    }

    @Override
    public void setSynchronizedBegin() {
        synchronizedBegin++;
    }

    @Override
    public void resetCommitted() {
        committed = 0;
    }

    @Override
    public void resetPrepared() {
        prepared = 0;
    }

    @Override
    public void resetRolledback() {
        rolledback = 0;
    }

    @Override
    public void resetSynchronizedBefore() {
        synchronizedBefore = 0;
    }

    @Override
    public void resetSynchronizedAfter() {
        synchronizedAfter = 0;
        synchronizedAfterCommitted = 0;
        synchronizedAfterRolledBack = 0;
    }

    @Override
    public void resetSynchronizedBegin() {
        synchronizedBegin = 0;
    }

    @Override
    public int countSynchronizedBefore() {
        return synchronizedBefore;
    }

    @Override
    public int countSynchronizedAfter() {
        return synchronizedAfter;
    }

    @Override
    public int countSynchronizedAfterCommitted() {
        return synchronizedAfterCommitted;
    }

    @Override
    public int countSynchronizedAfterRolledBack() {
        return synchronizedAfterRolledBack;
    }

    @Override
    public int countSynchronizedBegin() {
        return synchronizedBegin;
    }

    @Override
    public void addMessage(String msg) {
        messages.add(msg);
    }

    @Override
    public Collection<String> getMessages() {
        return messages;
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
