/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
