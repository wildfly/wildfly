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
package org.jboss.as.testsuite.integration.osgi;

import java.util.Arrays;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.osgi.service.startlevel.StartLevel;

/**
 * OSGi integration test support.
 *
 * @author thomas.diesler@jboss.com
 * @since 24-May-2011
 */
public class OSGiTestSupport {

    protected void setStartLevel(BundleContext context, int level) {
        ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
        StartLevel startLevel = (StartLevel) context.getService(sref);
        startLevel.setStartLevel(level);
    }

    protected void setBundleStartLevel(BundleContext context, Bundle bundle, int level) {
        ServiceReference sref = context.getServiceReference(StartLevel.class.getName());
        StartLevel startLevel = (StartLevel) context.getService(sref);
        startLevel.setBundleStartLevel(bundle, level);
    }

    /**
     * Get an array of bundles for the given symbolic name and version range.
     * @see PackageAdmin#getBundles(String, String)
     */
    protected Bundle[] getBundles(BundleContext context, String symbolicName, String versionRange) {
        ServiceReference sref = context.getServiceReference(PackageAdmin.class.getName());
        PackageAdmin padmin = (PackageAdmin) context.getService(sref);
        Bundle[] bundles = padmin.getBundles(symbolicName, versionRange);
        return bundles;
    }

    /**
     * Get a single bundle for the given symbolic name and version range.
     * @see PackageAdmin#getBundles(String, String)
     */
    protected Bundle getBundle(BundleContext context, String symbolicName, String versionRange) {
        Bundle[] bundles = getBundles(context, symbolicName, versionRange);
        if (bundles == null)
            return null;
        if (bundles.length != 1)
            throw new IllegalStateException("Cannot obtain a single bundle, found: " + Arrays.asList(bundles));

        return bundles[0];
    }
}