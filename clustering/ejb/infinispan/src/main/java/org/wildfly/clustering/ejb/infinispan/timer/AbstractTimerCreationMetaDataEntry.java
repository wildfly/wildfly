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

package org.wildfly.clustering.ejb.infinispan.timer;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.wildfly.clustering.ejb.timer.TimerConfiguration;

/**
 * @author Paul Ferraro
 */
public abstract class AbstractTimerCreationMetaDataEntry<V> implements TimerCreationMetaData<V> {

    private final V context;
    private final Instant start;

    protected AbstractTimerCreationMetaDataEntry(V context, TimerConfiguration config) {
        this(context, config.getStart());
    }

    protected AbstractTimerCreationMetaDataEntry(V context, Instant start) {
        this.context = context;
        this.start = (start != null) ? start.truncatedTo(ChronoUnit.MILLIS) : null;
    }

    @Override
    public V getContext() {
        return this.context;
    }

    @Override
    public Instant getStart() {
        return this.start;
    }
}
