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

import java.util.Collection;

/**
 * Interface used as remote point to {@link TransactionCheckerSingleton} class
 * that is used for verification of test workflow.
 *
 * @author Ondra Chaloupka <ochaloup@redhat.com>
 */
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
