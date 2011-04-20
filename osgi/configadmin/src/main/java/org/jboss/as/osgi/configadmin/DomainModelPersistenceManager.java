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
import java.util.Map;
import java.util.Vector;

import org.apache.felix.cm.PersistenceManager;
import org.jboss.as.osgi.service.ConfigAdminService;
import org.jboss.as.osgi.service.ConfigAdminServiceImpl;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceReference;

/**
 * An implementation of the Apache Felix ConfigAdmin {@link PersistenceManager} that delegates
 * to the {@link ConfigAdminServiceImpl}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Dec-2010
 */
@SuppressWarnings("rawtypes")
public class DomainModelPersistenceManager implements PersistenceManager, BundleActivator {

    private ConfigAdminService configadminService;

    @Override
    @SuppressWarnings("unchecked")
    public void start(BundleContext context) throws Exception {

        // Get the ConfigAdminService
        ServiceReference sref = context.getServiceReference(ServiceContainer.class.getName());
        ServiceContainer serviceContainer = (ServiceContainer) context.getService(sref);
        ServiceController<?> controller = serviceContainer.getRequiredService(ConfigAdminServiceImpl.SERVICE_NAME);
        configadminService = (ConfigAdminService) controller.getValue();

        // Register the PersistenceManager
        Hashtable props = new Hashtable();
        props.put(Constants.SERVICE_RANKING, Integer.MAX_VALUE);
        context.registerService(PersistenceManager.class.getName(), this, props);
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        configadminService = null;
    }

    @Override
    public boolean exists(String pid) {
        return configadminService.hasConfiguration(pid);
    }

    @Override
    public Dictionary load(String pid) throws IOException {
        Dictionary<String, String> props = configadminService.getConfiguration(pid);
        return addStandardProperties(pid, props);
    }

    @Override
    public Enumeration getDictionaries() throws IOException {
        Vector<Dictionary> result = new Vector<Dictionary>();
        for (String pid : configadminService.getConfigurations()) {
            Dictionary<String, String> props = configadminService.getConfiguration(pid);
            result.add(addStandardProperties(pid, props));
        }
        return result.elements();
    }

    @Override
    @SuppressWarnings({ "unchecked" })
    public void store(String pid, Dictionary properties) throws IOException {
        configadminService.putConfiguration(pid, properties);
    }

    @Override
    public void delete(String pid) throws IOException {
        configadminService.removeConfiguration(pid);
    }

    @SuppressWarnings("unchecked")
    private Dictionary<String, String> addStandardProperties(final String pid, final Dictionary<String, String> props) {
        if (props == null)
            return new Hashtable<String, String>();
        Dictionary<String, String> result = copy(props);
        result.put(Constants.SERVICE_PID, pid);
        return result;
    }

    @SuppressWarnings("unchecked")
    private Dictionary copy(final Dictionary source) {

        Hashtable copy = new Hashtable();
        if (source instanceof Map) {
            copy.putAll((Map) source);
        } else {
            Enumeration keys = source.keys();
            while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                copy.put(key, source.get(key));
            }
        }
        return copy;
    }
}
