/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.shared;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Dominik Pospisil <dpospisi@redhat.com>
 */
public class RetryTaskExecutor<T> {

    private static final long DEFAULT_RETRY_DELAY = 1000;
    private static final int DEFAULT_RETRY_COUNT = 60;

    private Callable<T> task;

    public final T retryTask(Callable<T> task) throws TimeoutException {
        return retryTask(task, DEFAULT_RETRY_COUNT, DEFAULT_RETRY_DELAY);
    }

    public final T retryTask(Callable<T> task, int retryCount, long retryDelay) throws TimeoutException {

        while(retryCount > 0) {
            try {
                return task.call();
            } catch (Exception e) {

            }
            retryCount --;
            try {
                Thread.sleep(retryDelay);
            } catch (InterruptedException ioe) {}
        }
        throw new TimeoutException();
    }

}
