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

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;

import org.apache.commons.lang.SystemUtils;

/**
 * Simple Krb5LoginModule configuration.
 * 
 * @author Josef Cacek
 */
public class Krb5LoginConfiguration extends Configuration {

    /** The list with configuration entries. */
    private final AppConfigurationEntry[] configList = new AppConfigurationEntry[1];

    /**
     * Create a new Krb5LoginConfiguration. Neither principal nor keytab are not filled and JGSS credential type is initiator.
     * 
     * @throws MalformedURLException
     */
    public Krb5LoginConfiguration() throws MalformedURLException {
        this(null, null, false);
    }

    /**
     * Create a new Krb5LoginConfiguration with given principal name, keytab and credential type.
     * 
     * @param principal principal name, may be <code>null</code>
     * @param keyTab keytab file, may be <code>null</code>
     * @param acceptor flag for setting credential type. Set to true, if the authenticated subject should be acceptor (i.e.
     *        credsType=acceptor for IBM JDK, and storeKey=true for Oracle JDK)
     * @throws MalformedURLException
     */
    public Krb5LoginConfiguration(final String principal, final File keyTab, final boolean acceptor)
            throws MalformedURLException {
        final Map<String, Object> options = new HashMap<String, Object>();
        final String loginModule;
        if (SystemUtils.JAVA_VENDOR.startsWith("IBM")) {
            loginModule = "com.ibm.security.auth.module.Krb5LoginModule";
            if (keyTab != null) {
                options.put("useKeytab", keyTab.toURI().toString());
            }
            if (acceptor) {
                options.put("credsType", "acceptor");
            } else {
                options.put("noAddress", "true");
            }
        } else {
            loginModule = "com.sun.security.auth.module.Krb5LoginModule";
            if (keyTab != null) {
                options.put("keyTab", keyTab.getAbsolutePath());
                options.put("doNotPrompt", "true");
                options.put("useKeyTab", "true");
            }
            if (acceptor) {
                options.put("storeKey", "true");
            }
        }
        options.put("refreshKrb5Config", "true");
        options.put("debug", "true");

        if (principal != null) {
            options.put("principal", principal);
        }
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
