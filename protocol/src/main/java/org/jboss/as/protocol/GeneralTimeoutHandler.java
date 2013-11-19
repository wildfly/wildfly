/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.protocol;

import java.util.concurrent.TimeUnit;

import org.xnio.IoFuture;
import org.xnio.IoFuture.Status;

/**
 * A general implementation of {@link ProtocolTimeoutHandler} that takes into account the time taken for {@link Runnable} tasks
 * to be executed.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class GeneralTimeoutHandler implements ProtocolTimeoutHandler {

    private volatile boolean thinking = false;
    private volatile long thinkTime = 0;

    public void suspendAndExecute(final Runnable runnable) {
        thinking = true;
        long startThinking = System.currentTimeMillis();
        try {
            runnable.run();
        } finally {
            thinkTime += System.currentTimeMillis() - startThinking;
            thinking = false;
        }
    }

    @Override
    public Status await(IoFuture<?> future, long timeoutMillis) {
        final long startTime = System.currentTimeMillis();

        IoFuture.Status status = future.await(timeoutMillis, TimeUnit.MILLISECONDS);
        while (status == IoFuture.Status.WAITING) {
            if (thinking) {
                status = future.await(timeoutMillis, TimeUnit.MILLISECONDS);
            } else {
                long timeToWait = (timeoutMillis + thinkTime) - (System.currentTimeMillis() - startTime);
                if (timeToWait > 0) {
                    status = future.await(timeToWait, TimeUnit.MILLISECONDS);
                } else {
                    return status;
                }
            }
        }

        return status;
    }

}