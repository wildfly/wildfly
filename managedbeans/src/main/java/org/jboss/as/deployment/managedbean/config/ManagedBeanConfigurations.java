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

package org.jboss.as.deployment.managedbean.config;

import org.jboss.as.deployment.AttachmentKey;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration class used to attach multiple ManagedBeanConfiguration objects to a deployment unit.  Allows multiple
 * processors to add configurations along the way.
 *
 * @author John E. Bailey
 */
public class ManagedBeanConfigurations implements Serializable{
    public static final AttachmentKey<ManagedBeanConfigurations> ATTACHMENT_KEY = AttachmentKey.create(ManagedBeanConfigurations.class);
    private static final long serialVersionUID = 821466962862896774L;

    /* Map of managed bean configurations mapped by type */
    private final Map<String, ManagedBeanConfiguration> configurations = new HashMap<String, ManagedBeanConfiguration>();

    /**
     * Get the configurations
     *
     * @return the configurations
     */
    public Map<String, ManagedBeanConfiguration> getConfigurations() {
        return Collections.unmodifiableMap(configurations);
    }

    /**
     * Add a managed bean configuration.
     *
     * @param managedBeanConfiguration the ManagedBeanConfiguration to add
     */
    public void add(final ManagedBeanConfiguration managedBeanConfiguration) {
        this.configurations.put(managedBeanConfiguration.getName(), managedBeanConfiguration);
    }

    /**
     * Check to see if the map contains an entry for the specified type.
     *
     * @param name the type to check
     * @return true if the type exists in the map, false if not
     */
    public boolean containsName(final String name) {
        return this.configurations.containsKey(name);
    }
}
