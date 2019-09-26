/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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
