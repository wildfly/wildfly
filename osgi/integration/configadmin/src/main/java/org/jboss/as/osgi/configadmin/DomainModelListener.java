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

import java.util.Dictionary;
import java.util.Set;

import org.apache.felix.cm.PersistenceManager;
import org.jboss.as.configadmin.ConfigAdminListener;
import org.jboss.as.configadmin.ConfigAdmin;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.util.tracker.ServiceTracker;

/**
 * An implementation of the Apache Felix ConfigAdmin {@link PersistenceManager} that delegates to the {@link ConfigAdmin}
 *
 * @author Thomas.Diesler@jboss.com
 * @since 01-Dec-2010
 */
class DomainModelListener implements ConfigAdminListener {

    static final String SKIP_CONFIGURATION_ADMIN_UPDATE = ".transient.skip.cm.update";

    private final ServiceTracker tracker;

    public DomainModelListener(BundleContext context) {
        tracker = new ServiceTracker(context, ConfigurationAdmin.class.getName(), null);
        tracker.open();
    }

    public void stop() {
        tracker.close();
    }

    @Override
    public void configurationModified(String pid, Dictionary<String, String> dictionary) {

        LOGGER.debugf("DomainModelListener modified %s => %s", pid, dictionary);
        ConfigurationAdmin configurationAdmin = (ConfigurationAdmin) tracker.getService();
        if (configurationAdmin == null) {
            LOGGER.debugf("No ConfigurationAdmin");
            return;
        }

        try {
            if (dictionary == null) {
                Configuration[] configs = configurationAdmin.listConfigurations("(" + Constants.SERVICE_PID + "=" + pid + ")");
                if (configs != null) {
                    // If the OSGi Configuration Admin Service has any configuration objects for this PID, delete them.
                    for (Configuration config : configs) {
                        config.delete();
                    }
                }
                return;
            } else {
                String skipUpdate = dictionary.get(SKIP_CONFIGURATION_ADMIN_UPDATE);
                if (!Boolean.parseBoolean(skipUpdate)) {
                    Configuration config = configurationAdmin.getConfiguration(pid, null);
                    config.update(dictionary);
                }
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
