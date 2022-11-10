/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.webservices.opensaml.sysconfig;

import java.security.PrivilegedAction;
import java.util.Properties;

import org.kohsuke.MetaInfServices;
import org.opensaml.core.config.ConfigurationPropertiesSource;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.SystemPropertyConfigurationPropertiesSource;

import static java.lang.System.getProperty;
import static java.security.AccessController.doPrivileged;

/**
 * A ConfigurationPropertiesSource implementation to fix https://issues.redhat.com/browse/WFLY-16650
 */
@MetaInfServices
public class SystemPropertiesSecMgrSource implements ConfigurationPropertiesSource {

    private SystemPropertyConfigurationPropertiesSource delegate = new SystemPropertyConfigurationPropertiesSource();
    private Properties empty = new Properties(0);
    private Properties patritionNameProperties = new Properties(1);

    public Properties getProperties() {
        try {
            return delegate.getProperties();
        } catch (SecurityException e) {
            String value = doPrivileged((PrivilegedAction<String>) () -> getProperty(ConfigurationService.PROPERTY_PARTITION_NAME));
            if (value == null) {
                return empty;
            } else {
                value = System.getProperty(ConfigurationService.PROPERTY_PARTITION_NAME);
                patritionNameProperties.setProperty(ConfigurationService.PROPERTY_PARTITION_NAME, value);
                return patritionNameProperties;
            }
        }
    }
}
