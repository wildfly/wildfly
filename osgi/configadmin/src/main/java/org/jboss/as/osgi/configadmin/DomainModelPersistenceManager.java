/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.osgi.configadmin;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import org.apache.felix.cm.PersistenceManager;
import org.jboss.as.configadmin.service.ConfigAdminListener;
import org.jboss.as.configadmin.service.ConfigAdminService;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An implementation of the Apache Felix ConfigAdmin {@link PersistenceManager} that delegates
 * to the {@link ConfigAdminService}
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 01-Dec-2010
 */
public class DomainModelPersistenceManager implements PersistenceManager, BundleActivator, ConfigAdminListener {
    private ConfigAdminService jbossConfigAdminService;
    private ServiceTracker osgiConfigAdminServiceTracker;

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void start(BundleContext context) throws Exception {
        osgiConfigAdminServiceTracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        osgiConfigAdminServiceTracker.open();

        // Get the ConfigAdminService
        ServiceReference sref = context.getServiceReference(ServiceContainer.class.getName());
        ServiceContainer serviceContainer = (ServiceContainer) context.getService(sref);
        ServiceController<?> controller = serviceContainer.getRequiredService(ConfigAdminService.SERVICE_NAME);
        jbossConfigAdminService = (ConfigAdminService) controller.getValue();
        jbossConfigAdminService.addListener(this);

        // Register the PersistenceManager
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        context.registerService(PersistenceManager.class.getName(), this, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        jbossConfigAdminService.removeListener(this);
        jbossConfigAdminService = null;

        osgiConfigAdminServiceTracker.close();
    }

    @Override
    public boolean exists(String pid) {
        return jbossConfigAdminService.hasConfiguration(pid);
    }

    @Override
    public Dictionary<String, String> load(String pid) throws IOException {
        Dictionary<String, String> props = jbossConfigAdminService.getConfiguration(pid);
        return addStandardProperties(pid, props);
    }

    @Override
    public Enumeration<Dictionary<String, String>> getDictionaries() throws IOException {
        Vector<Dictionary<String, String>> result = new Vector<Dictionary<String, String>>();
        for (String pid : jbossConfigAdminService.getConfigurations()) {
            Dictionary<String, String> props = jbossConfigAdminService.getConfiguration(pid);
            result.add(addStandardProperties(pid, props));
        }
        return result.elements();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void store(String pid, Dictionary source) throws IOException {
        if (ConfigAdminService.FROM_DMR_SOURCE_VALUE.equals(source.get(ConfigAdminService.SOURCE_PROPERTY_KEY))) {
            // If the change was triggered from the DMR, the persistence in the model was already done,
            // don't do it again.
            return;
        }

        Dictionary<String, String> copy = new Hashtable<String, String>();
        if (source != null) {
            Enumeration keys = source.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement().toString();
                copy.put(key, source.get(key).toString());
            }
        }
        jbossConfigAdminService.putConfiguration(pid, copy);
    }

    @Override
    public void delete(String pid) throws IOException {
        jbossConfigAdminService.removeConfiguration(pid);
    }

    private Dictionary<String, String> addStandardProperties(final String pid, final Dictionary<String, String> source) {
        Dictionary<String, String> copy = new Hashtable<String, String>();
        if (source != null) {
            Enumeration<String> keys = source.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                copy.put(key, source.get(key));
            }
        }
        copy.put(Constants.SERVICE_PID, pid);
        return copy;
    }

    @Override
    public void configurationModified(String pid, Dictionary<String,String> dictionary) {
        ConfigurationAdmin osgiConfigAdminService = (ConfigurationAdmin) osgiConfigAdminServiceTracker.getService();
        if (osgiConfigAdminService == null) {
            // No Configuration Admin Service available, nothing to do
            return;
        }

        try {
            Configuration configuration = osgiConfigAdminService.getConfiguration(pid, null);
            if (ConfigAdminService.FROM_DMR_SOURCE_VALUE.equals(dictionary.get(ConfigAdminService.SOURCE_PROPERTY_KEY))) {
                // Only call update if the change did not come from the OSGi Config Admin Service,
                // otherwise we end up in an infinite loop
                configuration.update(dictionary);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Set<String> getPIDs() {
        return null; // all PIDs
    }
}
