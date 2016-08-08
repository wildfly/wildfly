/*
 * JBoss, Home of Professional Open Source
 * Copyright 2005, JBoss Inc., and individual contributors as indicated
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
package org.jboss.as.test.osgi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.service.startlevel.StartLevel;
import org.osgi.util.tracker.ServiceTracker;


/**
 * OSGi integration test support.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-May-2011
 */
public final class FrameworkUtils {

    // Hide ctor
    private FrameworkUtils() {
    }

    public static void changeStartLevel(final BundleContext context, final int level) throws InterruptedException, TimeoutException {
        changeStartLevel(context, level, 10, TimeUnit.SECONDS);
    }

    /**
     * Changes the framework start level and waits for the STARTLEVEL_CHANGED event
     * Note, changing the framework start level is an asynchronous operation.
     */
    public static void changeStartLevel(final BundleContext context, final int level, final long timeout, final TimeUnit units) throws InterruptedException, TimeoutException {
        final ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
        final StartLevel startLevel = (StartLevel) context.getService(sref);
        if (level != startLevel.getStartLevel()) {
            final CountDownLatch latch = new CountDownLatch(1);
            context.addFrameworkListener(new FrameworkListener() {
                public void frameworkEvent(FrameworkEvent event) {
                    if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED && level == startLevel.getStartLevel()) {
                        latch.countDown();
                    }
                }
            });
            startLevel.setStartLevel(level);
            if (latch.await(timeout, units) == false)
                throw new TimeoutException("Timeout changing start level");
        }
    }

    public static <T> T waitForService(BundleContext context, Class<T> clazz) {
        return waitForService(context, clazz, 10, TimeUnit.SECONDS);
    }

    @SuppressWarnings("unchecked")
    public static <T> T waitForService(BundleContext context, Class<T> clazz, long timeout, TimeUnit unit) {
        ServiceReference sref = waitForServiceReference(context, clazz, timeout, unit);
        T service = sref != null ? (T) context.getService(sref) : null;
        Assert.assertNotNull("Service registered: " + clazz.getName(), service);
        return service;
    }

    public static ServiceReference waitForServiceReference(BundleContext context, Class<?> clazz) {
        return waitForServiceReference(context, clazz, 10, TimeUnit.SECONDS);
    }

    public static ServiceReference waitForServiceReference(BundleContext context, Class<?> clazz, long timeout, TimeUnit unit) {
        ServiceTracker tracker = new ServiceTracker(context, clazz.getName(), null);
        tracker.open();

        ServiceReference sref = null;
        try {
            if (tracker.waitForService(unit.toMillis(timeout)) != null) {
                sref = context.getServiceReference(clazz.getName());
            }
        } catch (InterruptedException e) {
            // service will be null
        } finally {
            tracker.close();
        }

        Assert.assertNotNull("Service registered: " + clazz.getName(), sref);
        return sref;
    }
}