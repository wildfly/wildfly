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
package org.jboss.as.test.integration.security.common;

import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

/**
 * Simple Krb5LoginModule configuration.
 * 
 * @author Josef Cacek
 */
public class Krb5LoginConfiguration extends Configuration {

    /** The list with configuration entries. */
    private static final AppConfigurationEntry[] configList = new AppConfigurationEntry[1];

    static {
        final Map<String, Object> options = new HashMap<String, Object>();
        final String loginModule;
        if (System.getProperty("java.vendor").startsWith("IBM")) {
            loginModule = "com.ibm.security.auth.module.Krb5LoginModule";
            options.put("noAddress", "true");
        } else {
            loginModule = "com.sun.security.auth.module.Krb5LoginModule";
        }
        options.put("refreshKrb5Config", "true");

        configList[0] = new AppConfigurationEntry(loginModule, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
    }

    /**
     * Interface method requiring us to return all the LoginModules we know about.
     * 
     * @param applicationName the application name
     * @return the configuration entry
     */
    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
        // We will ignore the applicationName, since we want all apps to use Kerberos V5
        return configList;
    }

}
