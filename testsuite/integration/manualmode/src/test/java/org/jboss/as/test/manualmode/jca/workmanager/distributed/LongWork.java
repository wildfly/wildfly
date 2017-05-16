/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.manualmode.jca.workmanager.distributed;

import javax.resource.spi.work.DistributableWork;
import java.io.Serializable;

public class LongWork implements DistributableWork, Serializable {

    private boolean quit = false;

    /**
     * Note that some distributed workmanager threads may depend on the exact time long work takes.
     */
    public static final long WORK_TIMEOUT = 1000L; // 1 second
    public static final long SLEEP_STEP = 50L; // 0.05 seconds

    @Override
    public void release() {
        quit = true;
    }

    @Override
    public void run() {
        long startTime = System.currentTimeMillis();
        long finishTime = startTime + WORK_TIMEOUT;

        while (!quit) {
            try {
                Thread.sleep(SLEEP_STEP);
            } catch (InterruptedException ignored) {
                // ignored
            }
            if (System.currentTimeMillis() >= finishTime) quit = true;
        }
    }
}
