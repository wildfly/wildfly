/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.pojo;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.StartException;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.junit.Assert.fail;

/**
 * @author John E. Bailey
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class TestBeanListener extends AbstractServiceListener<Object> {
    private volatile int count = 1;
    private final Runnable finishTask;

    private static final AtomicIntegerFieldUpdater<TestBeanListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(TestBeanListener.class, "count");

    public TestBeanListener(Runnable finishTask) {
        this.finishTask = finishTask;
    }

    public void listenerAdded(final ServiceController<? extends Object> serviceController) {
        countUpdater.incrementAndGet(this);
    }

    public void serviceStarted(final ServiceController<? extends Object> serviceController) {
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
        serviceController.removeListener(this);
    }

    public void serviceFailed(ServiceController<? extends Object> serviceController, StartException reason) {
        reason.printStackTrace();
        fail("Service failed to start: " + serviceController.getName());
    }

    public void finishBatch() {
        if (countUpdater.decrementAndGet(this) == 0) {
            batchComplete();
        }
    }

    private void batchComplete() {
        finishTask.run();
    }
}

