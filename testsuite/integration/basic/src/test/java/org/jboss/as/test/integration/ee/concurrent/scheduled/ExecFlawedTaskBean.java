/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.ee.concurrent.scheduled;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.enterprise.concurrent.ManagedScheduledExecutorService;

@Singleton
@Startup
public class ExecFlawedTaskBean implements ExecNumber {

    @Resource
    private ManagedScheduledExecutorService managedScheduledExecutorService;

    private ScheduledFuture future;

    private int count;
    private final int expected = 3;

    @Override
    public void cease() {
        if (this.future != null) {
            this.future.cancel(false);
        }
    }

    public void start() {
        future = managedScheduledExecutorService.scheduleWithFixedDelay(myTask, 0, 100, TimeUnit.MILLISECONDS);
    }

    @Override
    public int actual() {
        return this.count;
    }

    @Override
    public int expected() {
        return this.expected;
    }

    @PreDestroy
    private void preDestroy() {
        if (this.future != null) {
            this.future.cancel(true);
        }
    }

    private Runnable myTask = () -> {
        count++;
        if (count == this.expected) {
            throw new RuntimeException("Throwing exception to suppress subsequent executions.");
        }

    };
}
