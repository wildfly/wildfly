/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.ejb3.component.invocationmetrics;

import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author <a href="mailto:cdewolf@redhat.com">Carlo de Wolf</a>
 */
public class InvocationMetrics {
    private static class Values {
        final long invocations;
        final long executionTime;
        final long waitTime;

        private Values(final long invocations, final long waitTime, final long executionTime) {
            this.invocations = invocations;
            this.executionTime = executionTime;
            this.waitTime = waitTime;
        }
    }

    private final AtomicReference<Values> values = new AtomicReference<Values>(new Values(0, 0, 0));
    private final AtomicLong concurrent = new AtomicLong(0);
    private final AtomicLong peakConcurrent = new AtomicLong(0);

    void finishInvocation(final long invocationWaitTime, final long invocationExecutionTime) {
        concurrent.decrementAndGet();
        for(;;) {
            final Values oldv = values.get();
            final Values newv = new Values(oldv.invocations + 1, oldv.waitTime + invocationWaitTime, oldv.executionTime + invocationExecutionTime);
            if (values.compareAndSet(oldv, newv))
                return;
        }
    }

    public long getConcurrent() {
        return concurrent.get();
    }

    public long getExecutionTime() {
        return values.get().executionTime;
    }

    public long getInvocations() {
        return values.get().invocations;
    }

    public long getPeakConcurrent() {
        return peakConcurrent.get();
    }

    public long getWaitTime() {
        return values.get().waitTime;
    }

    void startInvocation() {
        final long v = concurrent.incrementAndGet();
        // concurrent might decrement here, but we take that missing peak for granted.
        if (peakConcurrent.get() < v)
            peakConcurrent.incrementAndGet();
    }
}
