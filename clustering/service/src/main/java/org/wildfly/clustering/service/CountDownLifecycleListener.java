/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.service;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;

/**
 * {@link LifecycleListener} that counts down a latch when a target {@link LifecycleEvent} is triggered.
 * @author Paul Ferraro
 */
public class CountDownLifecycleListener implements LifecycleListener {
    private final Set<LifecycleEvent> targetEvents;
    private final CountDownLatch latch;

    public CountDownLifecycleListener(CountDownLatch latch) {
        this(latch, EnumSet.allOf(LifecycleEvent.class));
    }

    public CountDownLifecycleListener(CountDownLatch latch, Set<LifecycleEvent> targetEvents) {
        this.targetEvents = targetEvents;
        this.latch = latch;
    }

    @Override
    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
        if (this.targetEvents.contains(event)) {
            this.latch.countDown();
        }
    }
}
