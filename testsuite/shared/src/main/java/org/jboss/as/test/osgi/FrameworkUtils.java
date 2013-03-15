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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Assert;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.VersionRange;
import org.osgi.framework.startlevel.FrameworkStartLevel;
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

    public static Bundle[] getBundles(BundleContext context, String symbolicName, VersionRange versionRange) {
        List<Bundle> result = new ArrayList<Bundle>();
        if (Constants.SYSTEM_BUNDLE_SYMBOLICNAME.equals(symbolicName) && versionRange == null) {
            result.add(context.getBundle(0));
        } else {
            for (Bundle aux : context.getBundles()) {
                if (symbolicName == null || symbolicName.equals(aux.getSymbolicName())) {
                    if (versionRange == null || versionRange.includes(aux.getVersion())) {
                        result.add(aux);
                    }
                }
            }
        }
        return !result.isEmpty() ? result.toArray(new Bundle[result.size()]) : null;
    }

    public static int getFrameworkStartLevel(final BundleContext context)  {
        return context.getBundle().adapt(FrameworkStartLevel.class).getStartLevel();
    }

    public static void setFrameworkStartLevel(final BundleContext context, final int level) throws InterruptedException, TimeoutException {
        setFrameworkStartLevel(context, level, 10, TimeUnit.SECONDS);
    }

    /**
     * Changes the framework start level and waits for the STARTLEVEL_CHANGED event
     * Note, changing the framework start level is an asynchronous operation.
     */
    public static void setFrameworkStartLevel(final BundleContext context, final int level, final long timeout, final TimeUnit units) throws InterruptedException, TimeoutException {
        final FrameworkStartLevel startLevel = context.getBundle().adapt(FrameworkStartLevel.class);
        if (level != startLevel.getStartLevel()) {
            final CountDownLatch latch = new CountDownLatch(1);
            FrameworkListener listener = new FrameworkListener() {
                public void frameworkEvent(FrameworkEvent event) {
                    if (event.getType() == FrameworkEvent.STARTLEVEL_CHANGED && level == startLevel.getStartLevel()) {
                        latch.countDown();
                    }
                }
            };
            startLevel.setStartLevel(level, listener);
            if (latch.await(timeout, units) == false)
                throw new TimeoutException("Timeout changing start level");
        }
    }

    public static <T> T waitForService(BundleContext context, Class<T> clazz) {
        return waitForService(context, clazz, 10, TimeUnit.SECONDS);
    }

    public static <T> T waitForService(BundleContext context, Class<T> clazz, long timeout, TimeUnit unit) {
        ServiceReference<T> sref = waitForServiceReference(context, clazz, timeout, unit);
        T service = sref != null ? context.getService(sref) : null;
        Assert.assertNotNull("Service registered: " + clazz.getName(), service);
        return service;
    }

    public static <T> ServiceReference<T> waitForServiceReference(BundleContext context, Class<T> clazz) {
        return waitForServiceReference(context, clazz, 10, TimeUnit.SECONDS);
    }

    public static <T> ServiceReference<T> waitForServiceReference(BundleContext context, Class<T> clazz, long timeout, TimeUnit unit) {
        ServiceTracker tracker = new ServiceTracker(context, clazz.getName(), null);
        tracker.open();

        ServiceReference<T> sref = null;
        try {
            if (tracker.waitForService(unit.toMillis(timeout)) != null) {
                sref = context.getServiceReference(clazz);
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