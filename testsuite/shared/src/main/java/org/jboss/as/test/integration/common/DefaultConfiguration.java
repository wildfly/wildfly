/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.test.integration.common;

import org.jboss.as.test.http.Authentication;
import org.jboss.as.test.shared.integration.ejb.security.CallbackHandler;

import javax.management.remote.JMXConnector;
import javax.naming.Context;
import java.util.HashMap;
import java.util.Properties;

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
    * Returns the env Properties updated with security configurations for ejb connections
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