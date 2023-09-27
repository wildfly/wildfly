/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common;

import java.io.File;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;


/**
 * Simple Krb5LoginModule configuration.
 *
 * @author Josef Cacek
 */
public class Krb5LoginConfiguration extends Configuration {

    /** The list with configuration entries. */
    private final AppConfigurationEntry[] configList = new AppConfigurationEntry[1];
    private final String name;
    private final Configuration wrapped;

    /**
     * Create a new Krb5LoginConfiguration. Neither principal nor keytab are not filled and JGSS credential type is initiator.
     *
     * @throws MalformedURLException
     */
    public Krb5LoginConfiguration(final Configuration wrapped) throws MalformedURLException {
        this(null, null, false, wrapped);
    }

    /**
     * Create a new Krb5LoginConfiguration with given principal name, keytab and credential type.
     *
     * @param principal principal name, may be <code>null</code>
     * @param keyTab keytab file, may be <code>null</code>
     * @param acceptor flag for setting credential type. Set to true, if the authenticated subject should be acceptor (i.e.
     *        credsType=acceptor for IBM JDK, and storeKey=true for Oracle JDK)
     * @param wrapped wrapped configuration (you can receive it for instance by calling Configuration#getConfiguration()
     * @throws MalformedURLException
     */
    public Krb5LoginConfiguration(final String principal, final File keyTab, final boolean acceptor, final Configuration wrapped)
            throws MalformedURLException {
        final String loginModule = getLoginModule();
        Map<String, String> options = getOptions(principal, keyTab, acceptor);
        configList[0] = new AppConfigurationEntry(loginModule, AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);
        name = UUID.randomUUID().toString();
        this.wrapped = wrapped;
    }

    /**
     * Returns Map with Krb5LoginModule options. The result depends on currently running JVM.
     *
     * @param principal principal name, may be <code>null</code>
     * @param keyTab keytab file, may be <code>null</code>
     * @param acceptor flag for setting credential type. Set to true, if the authenticated subject should be acceptor (i.e.
     *        credsType=acceptor for IBM JDK, and storeKey=true for Oracle JDK)
     * @return HashMap with Krb5LoginModule options.
     */
    public static Map<String, String> getOptions(final String principal, final File keyTab, final boolean acceptor) {
        final Map<String, String> res = new HashMap<String, String>();

        if (keyTab != null) {
            res.put("keyTab", keyTab.getAbsolutePath());
            res.put("doNotPrompt", "true");
            res.put("useKeyTab", "true");
        }
        if (acceptor) {
            res.put("storeKey", "true");
        }

        res.put("refreshKrb5Config", "true");
        //res.put("debug", "true");

        if (principal != null) {
            res.put("principal", principal);
        }

        return res;
    }

    /**
     * Returns Krb5LoginModule class name. The returned name depends on the currently running JVM.
     *
     * @return class name
     */
    public static String getLoginModule() {
        return "com.sun.security.auth.module.Krb5LoginModule";
    }

    /**
     * Returns this login configuration name.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the wrapped configuration.
     *
     * @return
     */
    protected Configuration getWrapped() {
        return wrapped;
    }

    /**
     * Interface method requiring us to return all the LoginModules we know about.
     *
     * @param applicationName the application name
     * @return the configuration entry
     */
    @Override
    public AppConfigurationEntry[] getAppConfigurationEntry(String applicationName) {
        if (name.equals(applicationName)) {
            // We will ignore the applicationName, since we want all apps to use Kerberos V5
            return configList;
        } else {
            return wrapped == null ? null : wrapped.getAppConfigurationEntry(applicationName);
        }
    }

    /**
     * Resets configuration to the wrapped one and returns it.
     *
     * @return login configuration to which it was reseted
     */
    public Configuration resetConfiguration() {
        Configuration.setConfiguration(wrapped);
        return wrapped;
    }

}
