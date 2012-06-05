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
package org.jboss.as.configadmin.service;

import java.util.Dictionary;
import java.util.Set;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;

/**
 * Maintains a set of {@link Dictionary}s in the domain model keyed by persistent ID (PID).
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Nov-2010
 */
public interface ConfigAdminService extends Service<ConfigAdminService> {
    // The SOURCE_PROPERTY_KEY is a private property used on the Config Admin entries to
    // avoid inifinite loops. There are multiple potential sources of configuration updates:
    // 1. The DMR API
    // 2. The OSGi Configuration Admin Service
    // 3. The MSC Config Admin Sevice
    // Since updates from the DMR need to be propagated to the services and vice versa
    // this source property is used to stop the system from looping around forever.
    String SOURCE_PROPERTY_KEY = ".org.jboss.source";
    String FROM_DMR_SOURCE_VALUE = "fromdmr";
    String FROM_NONDMR_SOURCE_VALUE = "notfromdmr";

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("configadmin");

    /**
     * Get the set of registered PIDs
     * @return The set of registered PIDs or an empty set.
     */
    Set<String> getConfigurations();

    /**
     * True if there is a configuration for the given PID.
     */
    boolean hasConfiguration(String pid);

    /**
     * Get the configuration dictionary for the given PID.
     * @return The configuration dictionary or <code>null</code>
     */
    Dictionary<String, String> getConfiguration(String pid);

    /**
     * Put or update the configuration for the given PID.
     * @return The previously registered configuration or <code>null</code>
     */
    Dictionary<String, String> putConfiguration(String pid, Dictionary<String, String> config);

    /**
     * Remove the configuration for the given PID.
     * @return The previously registered configuration or <code>null</code>
     */
    Dictionary<String, String> removeConfiguration(String pid);

    /**
     * Add a configuration listener.
     */
    void addListener(ConfigAdminListener listener);

    /**
     * Remove a configuration listener.
     */
    void removeListener(ConfigAdminListener listener);
}