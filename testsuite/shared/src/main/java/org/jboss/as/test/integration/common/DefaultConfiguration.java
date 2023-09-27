/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.test.integration.common;

import java.util.HashMap;
import java.util.Properties;
import javax.management.remote.JMXConnector;
import javax.naming.Context;

import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;

/**
 * Created by fspolti on 10/20/16.
 * WFLY-7309 / JBEAP-6424
 */
public class DefaultConfiguration {
    private static final boolean isRemote = Boolean.parseBoolean(System.getProperty("org.jboss.as.test.integration.remote", "false"));
    private static final String MBEAN_USERNAME = System.getProperty("jboss.mbean.username", Authentication.USERNAME);
    private static final String MBEAN_PASSWORD = System.getProperty("jboss.mbean.username", Authentication.PASSWORD);
    private static final String APPLICATION_USERNAME = System.getProperty("jboss.application.username", "guest");
    private static final String APPLICATION_PASSWORD = System.getProperty("jboss.application.username", "guest");

    /*
    * Return the value of isRemote variable, it will be used to define
    * if the tests are abring running in a local or remote container. If no value
    * is provided then the default value is false (local)
    */
    public static boolean isRemote() {
        return isRemote;
    }

    /*
    * Return the application username, if none is provided it will return the default:
    * guest
    */
    public static String applicationUsername() {
        return APPLICATION_USERNAME;
    }

    /*
    * Return the application password, if none is provided it will return the default:
    * guest
    */
    public static String applicationPassword() {
        return APPLICATION_PASSWORD;
    }

    /*
    * Returns a HashMap containing the credentials for a JMX connection
    * default user and pass is testSuite/testSuitePassword
    */
    public static HashMap<String, String[]> credentials () {
        HashMap<String, String[]> propEnv = new HashMap<String, String[]>();
        String[] credentials = {  MBEAN_USERNAME, MBEAN_PASSWORD };
        propEnv.put(JMXConnector.CREDENTIALS, credentials);
        return isRemote() ? propEnv : null;
    }

    /*
    * Returns the env Properties updated with security configurations for Jakarta Enterprise Beans connections
    * @param env the properties to be updated
    * @return the Properties updated
    */
    public static Properties addSecurityProperties(Properties env) {
        if (isRemote()){
            env.put(Context.SECURITY_PRINCIPAL, applicationUsername());
            env.put(Context.SECURITY_CREDENTIALS, applicationPassword());
        } else {
            env.put("jboss.naming.client.security.callback.handler.class", CallbackHandler.class.getName());
        }
        return env;
    }
}
