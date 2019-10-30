/*
 * JBoss, Home of Professional Open Source
 * Copyright 2016, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.integration.transaction.inflow;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.resource.spi.work.WorkEvent;
import javax.resource.spi.work.WorkListener;

/**
 * Simple listener to monitor if work was done already.
 *
 * @author Ondrej Chaloupka <ochaloup@redhat.com>
 */
public class TransactionInflowWorkListener implements WorkListener {
    private AtomicBoolean isAccepted = new AtomicBoolean(false);
    private AtomicBoolean isRejected = new AtomicBoolean(false);
    private AtomicBoolean isStarted = new AtomicBoolean(false);
    private AtomicBoolean isCompleted = new AtomicBoolean(false);

    @Override
    public void workAccepted(WorkEvent e) {
        isAccepted.set(true);
    }

    @Override
    public void workRejected(WorkEvent e) {
        isRejected.set(true);
    }

    @Override
    public void workStarted(WorkEvent e) {
        isStarted.set(true);
    }

    @Override
    public void workCompleted(WorkEvent e) {
        isCompleted.set(true);
    }

    boolean isAccepted() {
        return isAccepted.get();
    }

    boolean isRejected() {
        return isRejected.get();
    }

    boolean isStarted() {
        return isStarted.get();
    }

    boolean isCompleted() {
        return isCompleted.get();
    }
}
