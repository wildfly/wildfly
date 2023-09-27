/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
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
