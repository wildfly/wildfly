/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
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
package org.jboss.as.ejb3.tx;


import org.wildfly.common.Assert;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * A lock that supports reentrancy based on owner (and not on current thread).
 *
 * @author Stuart Douglas
 */
public class OwnableReentrantLock {

    private static final long serialVersionUID = 493297473462848792L;

    /**
     * Current owner
     */
    private Object owner;

    private final Object lock = new Object();

    private int lockCount = 0;

    private int waiters = 0;

    /**
     * Creates a new lock instance.
     */
    public OwnableReentrantLock() {
    }

    public void lock(Object owner) {
        Assert.checkNotNullParam("owner", owner);
        synchronized (this.lock) {
            if (Objects.equals(owner, this.owner)) {
                lockCount++;
            } else if (this.owner == null) {
                this.owner = owner;
                lockCount ++;
            } else {
                while (this.owner != null) {
                    try {
                        waiters++;
                        try {
                            lock.wait();
                        } finally {
                            --waiters;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                this.owner = owner;
                lockCount ++;
            }

        }
    }


    public boolean tryLock(long timeValue, TimeUnit timeUnit, Object owner) {
        Assert.checkNotNullParam("owner", owner);
        synchronized (this.lock) {
            if (Objects.equals(owner, this.owner)) {
                lockCount++;
                return true;
            } else if (this.owner == null) {
                this.owner = owner;
                lockCount ++;
                return true;
            } else {
                long endTime = System.currentTimeMillis() + timeUnit.toMillis(timeValue);
                while (this.owner != null && System.currentTimeMillis() < endTime) {
                    try {
                        waiters++;
                        try {
                            lock.wait(endTime - System.currentTimeMillis());
                        } finally {
                            waiters--;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(this.owner == null) {
                    this.owner = owner;
                    lockCount++;
                    return true;
                } else {
                    return false;
                }
            }

        }
    }

    public void unlock(Object owner) {
        Assert.checkNotNullParam("owner", owner);
        synchronized (this.lock) {
            if (!Objects.equals(owner,this.owner)) {
                throw new IllegalMonitorStateException();
            } else {
                if (--lockCount == 0) {
                    this.owner = null;
                    if (waiters > 0) {
                        lock.notifyAll();
                    }
                }
            }
        }
    }

    /**
     * Returns a string identifying this lock, as well as its lock state.  The state, in brackets, includes either the
     * String &quot;Unlocked&quot; or the String &quot;Locked by&quot; followed by the String representation of the lock
     * owner.
     *
     * @return a string identifying this lock, as well as its lock state.
     */
    public String toString() {
        return super.toString() + ((owner == null) ?
                "[Unlocked]" :
                "[Locked by " + owner + "]");
    }
}
