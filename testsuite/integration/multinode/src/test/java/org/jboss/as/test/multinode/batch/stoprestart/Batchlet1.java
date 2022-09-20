/*
 * JBoss, Home of Professional Open Source
 * Copyright 2021, Red Hat Inc., and individual contributors as indicated
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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

package org.jboss.as.test.multinode.batch.stoprestart;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import jakarta.batch.api.BatchProperty;
import jakarta.batch.api.Batchlet;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.inject.Named;

@Named
@Dependent
public class Batchlet1 implements Batchlet {
    private final AtomicBoolean stopRequested = new AtomicBoolean();

    @Inject
    @BatchProperty
    long seconds;

    @Inject
    @BatchProperty
    int interval;

    @Override
    public String process() throws Exception {
        if (seconds > 0) {
            long startTime = System.currentTimeMillis();
            long targetDuration = seconds * 1000;
            long sleepAmount;
            while((sleepAmount = System.currentTimeMillis() - startTime) < targetDuration && !stopRequested.get()) {
                Thread.sleep(interval);
            }
            return "Slept " + TimeUnit.MILLISECONDS.toSeconds(sleepAmount) + " seconds";
        }
        return "Direct return no sleep";
    }

    @Override
    public void stop() throws Exception {
        stopRequested.set(true);
    }
}
