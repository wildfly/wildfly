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

import java.io.IOException;
import java.util.Hashtable;

import org.jboss.as.test.integration.osgi.api.ConfiguredService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ManagedService;
import org.osgi.util.tracker.ServiceTracker;

/**
 * A simple activator that creates a configuration
 *
 * @author Thomas.Diesler@jboss.org
 * @since 12-Dec-2010
 */
public class ConfigAdminBundleActivatorB implements BundleActivator {

    private ServiceTracker tracker;

    @Override
    public void start(BundleContext context) throws Exception {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, context.getBundle().getLocation());
        context.registerService(new String[] { ManagedService.class.getName(), ConfiguredService.class.getName() }, new ConfiguredService(), props);

        tracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null) {
            @Override
            public Object addingService(ServiceReference reference) {
                ConfigurationAdmin service = (ConfigurationAdmin) super.addingService(reference);
                try {
                    Configuration config = service.getConfiguration(context.getBundle().getLocation());
                    Hashtable<String, String> props = new Hashtable<String, String>();
                    props.put("foo", "bar");
                    config.update(props);
                } catch (IOException ex) {
                    throw new IllegalStateException(ex);
                }
                return service;
            }
        };
        tracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        tracker.close();
    }
}
