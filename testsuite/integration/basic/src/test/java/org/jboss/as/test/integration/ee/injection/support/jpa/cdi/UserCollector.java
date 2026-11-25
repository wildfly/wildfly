/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.ee.injection.support.jpa.cdi;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.as.test.shared.TimeoutUtil;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@ApplicationScoped
public class UserCollector {

    private final Map<EventType, User> events = new EnumMap<>(EventType.class);
    private final ReentrantLock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public User pop(final EventType eventType) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            long nanos = TimeUnit.SECONDS.toNanos(TimeoutUtil.adjust(5));
            User value;
            while ((value = events.remove(eventType)) == null) {
                if (nanos <= 0) {
                    return value;
                }
                nanos = condition.awaitNanos(nanos);
            }
            return value;
        } finally {
            lock.unlock();
        }
    }

    public void push(final EventType eventType, final User user) {
        lock.lock();
        try {
            if (events.containsKey(eventType)) {
                throw new IllegalArgumentException(String.format("Event type %s already has an entry: %s", eventType, events.get(eventType)));
            }
            events.put(eventType, user);
            condition.signal();
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            events.clear();
        } finally {
            lock.unlock();
        }
    }
}
