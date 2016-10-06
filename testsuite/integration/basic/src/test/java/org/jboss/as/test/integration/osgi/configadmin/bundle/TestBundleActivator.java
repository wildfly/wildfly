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

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Dictionary;
import java.util.Hashtable;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

/**
 * This test bundle activator registers a Config Admin managed service that writes a value to a file
 * specified in its configuration update. The test that drives the bundle creates the configuration
 * information and then verifies the content of the specified file.
 *
 * @author David Bosschaert
 */
public class TestBundleActivator implements BundleActivator {
    private ServiceRegistration reg;

    @Override
    public void start(BundleContext context) throws Exception {
        ManagedService ms = new MyManagedService();
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(Constants.SERVICE_PID, getClass().getName());
        reg = context.registerService(ManagedService.class.getName(), ms, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        reg.unregister();
    }

    private class MyManagedService implements ManagedService {
        @Override
        @SuppressWarnings("rawtypes")
        public void updated(Dictionary props) throws ConfigurationException {
            if (props == null)
                return;

            String f = (String) props.get("file");
            if (f == null)
                return;

            String v = (String) props.get("value");
            if (v == null)
                return;

            OutputStream fos = null;
            try {
                fos = new FileOutputStream(f);
                fos.write(v.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
