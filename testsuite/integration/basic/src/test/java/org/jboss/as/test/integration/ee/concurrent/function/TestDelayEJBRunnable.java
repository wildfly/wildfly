/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.ee.concurrent.function;

import java.util.concurrent.CountDownLatch;

import org.jboss.as.test.integration.ee.concurrent.TestEJBRunnable;

/**
 * 
 * @author Hynek Svabek
 *
 */
public class TestDelayEJBRunnable extends TestEJBRunnable{

    private final long delay;
    private final CountDownLatch latch;
    
    public TestDelayEJBRunnable(long delayMillis) {
        this(delayMillis, null);
    }

    public TestDelayEJBRunnable(CountDownLatch latch) {
        this(0, latch);
    }
    
    public TestDelayEJBRunnable(long delayMillis, CountDownLatch latch) {
        this.delay = delayMillis;
        this.latch = latch;
    }
    
    @Override
    public void run() {
        if(delay > 0){
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
        super.run();
        if(latch != null){
            latch.countDown();
        }
    }    
}
