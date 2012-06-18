/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.osgi.configadmin.bundle;

import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * This test bundle activator write data to the OSGi Configuration Admin service. Once written, the
 * test code verifies that the data is available via the DMR interface.
 *
 * @author David Bosschaert
 */
public class TestBundleActivator2 implements BundleActivator {
    @Override
    public void start(BundleContext context) throws Exception {
        ServiceTracker st = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        st.open();
        try {
            ConfigurationAdmin cadmin = (ConfigurationAdmin) st.waitForService(30000);
            Configuration configuration = cadmin.getConfiguration(TestBundleActivator2.class.getName(), null);
            Dictionary<String, Object> dictionary = new Hashtable<String, Object>();
            dictionary.put("from.bundle", "initial");
            configuration.update(dictionary);
        } finally {
            st.close();
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        ServiceTracker st = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        st.open();
        try {
            ConfigurationAdmin cadmin = (ConfigurationAdmin) st.waitForService(30000);
            Configuration configuration = cadmin.getConfiguration(TestBundleActivator2.class.getName(), null);
            Dictionary<String, Object> d = new Hashtable<String, Object>();
            d.put("from.bundle", "updated");
            configuration.update(d);
        } finally {
            st.close();
        }
    }
}
