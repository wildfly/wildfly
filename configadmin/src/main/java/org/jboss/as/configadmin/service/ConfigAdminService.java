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
 * Maintains a set of {@link Dictionary}s in the domain model keyd by persistent ID (PID).
 *
 * @author Thomas.Diesler@jboss.com
 * @since 29-Nov-2010
 */
public interface ConfigAdminService extends Service<ConfigAdminService> {

    ServiceName SERVICE_NAME = ServiceName.JBOSS.append("configadmin");

    /**
     * Get the set of registered PIDs
     * @return The set of registered PIDs or an empty set.
     */
    Set<String> getConfigurations();

    /**
     * True if therer is a configuration for the given PID.
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
     * REmove the configuration for the given PID.
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