/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ejb.threadpool;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.Timer;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

@Startup
@Singleton
public class ScheduleSingletonOneTimer {
    private final ConcurrentSkipListSet<String> threadNames = new ConcurrentSkipListSet<String>();

    @Schedule(second="*/1", minute="*", hour="*", persistent=false)
    public void timeout(Timer t) {
        threadNames.add(Thread.currentThread().getName());
    }

    public Set<String> getThreadNames() {
        return Collections.unmodifiableSet(threadNames);
    }
}


