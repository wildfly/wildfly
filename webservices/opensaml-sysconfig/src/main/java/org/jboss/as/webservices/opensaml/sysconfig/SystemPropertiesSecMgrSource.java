/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.webservices.opensaml.sysconfig;

import java.security.PrivilegedAction;
import java.util.Properties;

import org.kohsuke.MetaInfServices;
import org.opensaml.core.config.ConfigurationProperties;
import org.opensaml.core.config.ConfigurationPropertiesSource;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.provider.EmptyConfigurationProperties;
import org.opensaml.core.config.provider.PropertiesAdapter;
import org.opensaml.core.config.provider.SystemPropertyConfigurationPropertiesSource;

import static java.lang.System.getProperty;
import static java.security.AccessController.doPrivileged;

/**
 * A ConfigurationPropertiesSource implementation to fix https://issues.redhat.com/browse/WFLY-16650
 */
@MetaInfServices
public class SystemPropertiesSecMgrSource implements ConfigurationPropertiesSource {

    private SystemPropertyConfigurationPropertiesSource delegate = new SystemPropertyConfigurationPropertiesSource();

    public ConfigurationProperties getProperties() {
        try {
            return delegate.getProperties();
        } catch (SecurityException e) {
            String value = doPrivileged((PrivilegedAction<String>) () -> getProperty(ConfigurationService.PROPERTY_PARTITION_NAME));
            if (value == null) {
                return new EmptyConfigurationProperties();
            } else {
                value = System.getProperty(ConfigurationService.PROPERTY_PARTITION_NAME);
                Properties partitionNameProperties = new Properties(1);
                partitionNameProperties.setProperty(ConfigurationService.PROPERTY_PARTITION_NAME, value);
                return new PropertiesAdapter(partitionNameProperties);
            }
        }
    }
}
