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

import static org.jboss.as.configadmin.ConfigAdminLogger.LOGGER;

import java.io.IOException;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.apache.felix.cm.PersistenceManager;
import org.jboss.as.configadmin.ConfigAdmin;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * An implementation of the Apache Felix ConfigAdmin {@link PersistenceManager} that delegates to the {@link ConfigAdmin}
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @since 01-Dec-2010
 */
public class DomainModelPersistenceManager implements PersistenceManager, BundleActivator {

    private ConfigAdmin configAdminService;
    private DomainModelListener configAdminListener;

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void start(BundleContext context) throws Exception {

        // Register the PersistenceManager
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        context.registerService(PersistenceManager.class.getName(), this, props);

        // Start the {@link ConfigAdminListener}
        configAdminListener = new DomainModelListener(context);

        // Get the {@link ConfigAdminService} and add a lister
        configAdminService = getConfigAdminService(context);
        configAdminService.addListener(configAdminListener);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        configAdminListener.stop();
        configAdminService.removeListener(configAdminListener);
    }

    @Override
    public boolean exists(String pid) {
        return configAdminService.hasConfiguration(pid);
    }

    @Override
    public Dictionary<String, String> load(String pid) throws IOException {
        Dictionary<String, String> props = configAdminService.getConfiguration(pid);
        Dictionary<String, String> result = addStandardProperties(pid, props);
        LOGGER.debugf("PM load %s => %s", pid, result);
        return result;
    }

    @Override
    public Enumeration<Dictionary<String, String>> getDictionaries() throws IOException {
        Vector<Dictionary<String, String>> result = new Vector<Dictionary<String, String>>();
        for (String pid : configAdminService.getConfigurations()) {
            Dictionary<String, String> props = configAdminService.getConfiguration(pid);
            result.add(addStandardProperties(pid, props));
        }
        return result.elements();
    }

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void store(String pid, Dictionary source) throws IOException {
        //if (!"true".equals(source.get(DomainModelListener.SKIP_CONFIGURATION_STORE))) {
            Dictionary<String, String> dictionary = removeUnsupportedProperties(source);
            dictionary.put(DomainModelListener.SKIP_CONFIGURATION_ADMIN_UPDATE, "true");
            LOGGER.debugf("PM store %s => %s", pid, dictionary);
            configAdminService.putConfiguration(pid, dictionary);
        //}
    }

    @Override
    public void delete(String pid) throws IOException {
        LOGGER.debugf("PM delete %s", pid);
        configAdminService.removeConfiguration(pid);
    }

    private ConfigAdmin getConfigAdminService(BundleContext context) {
        ServiceReference sref = context.getServiceReference(ServiceContainer.class.getName());
        ServiceContainer serviceContainer = (ServiceContainer) context.getService(sref);
        ServiceController<?> controller = serviceContainer.getRequiredService(ConfigAdmin.SERVICE_NAME);
        return (ConfigAdmin) controller.getValue();
    }

    private Dictionary<String, String> addStandardProperties(String pid, Dictionary<String, String> source) {
        Dictionary<String, String> copy = getModifiableDictionary(source);
        copy.put(Constants.SERVICE_PID, pid);
        return copy;
    }

    private Dictionary<String, String> removeUnsupportedProperties(Dictionary<String, Object> source) {
        Dictionary<String, String> copy = new Hashtable<String, String>();
        Enumeration<String> keys = source.keys();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            Object value = source.get(key);
            if (value instanceof String) {
                copy.put(key, (String) value);
            }
        }
        return copy;
    }

    private Dictionary<String, String> removeStandardProperties(Dictionary<String, String> source) {
        Dictionary<String, String> copy = getModifiableDictionary(source);
        copy.remove("service.bundleLocation");
        copy.remove("service.pid");
        return copy;
    }

    private Dictionary<String, String> getModifiableDictionary(Dictionary<String, String> source) {
        Dictionary<String, String> result = new Hashtable<String, String>();
        if (source != null) {
            Enumeration<String> keys = source.keys();
            while (keys.hasMoreElements()) {
                String key = keys.nextElement();
                result.put(key, source.get(key));
            }
        }
        return result;
    }
}
