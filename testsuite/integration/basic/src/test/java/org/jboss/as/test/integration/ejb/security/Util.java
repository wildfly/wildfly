/*
 * JBoss, Home of Professional Open Source.
 * Copyright (c) 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.ejb.security;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.jboss.security.ClientLoginModule;

/**
 * Holder for couple of utility methods used while testing EJB3 security.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class Util {

    /**
     * Obtain a LoginContext configured for use with the ClientLoginModule.
     *
     * @return the configured LoginContext.
     */
    public static LoginContext getCLMLoginContext(final String username, final String password) throws LoginException {
        final String configurationName = "Testing";

        CallbackHandler cbh = new CallbackHandler() {
            public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
                for (Callback current : callbacks) {
                    if (current instanceof NameCallback) {
                        ((NameCallback) current).setName(username);
                    } else if (current instanceof PasswordCallback) {
                        ((PasswordCallback) current).setPassword(password.toCharArray());
                    } else {
                        throw new UnsupportedCallbackException(current);
                    }
                }
            }
        };
        Configuration config = new Configuration() {

            @Override
            public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
                if (configurationName.equals(name) == false) {
                    throw new IllegalArgumentException("Unexpected configuration name '" + name + "'");
                }
                Map<String, String> options = new HashMap<String, String>();
                options.put("multi-threaded", "true");
                options.put("restore-login-identity", "true");

                AppConfigurationEntry clmEntry = new AppConfigurationEntry(ClientLoginModule.class.getName(), AppConfigurationEntry.LoginModuleControlFlag.REQUIRED, options);

                return new AppConfigurationEntry[]{clmEntry};
            }
        };

        return new LoginContext(configurationName, new Subject(), cbh, config);
    }

    /**
     * Creates JNDI context string based on given parameters.
     * See details at https://docs.jboss.org/author/display/AS71/EJB+invocations+from+a+remote+client+using+JNDI
     *
     * @param appName - typically the ear name without the .ear
     *                - could be empty string when deploying just jar with EJBs
     * @param moduleName - jar file name without trailing .jar
     * @param distinctName - AS7 allows each deployment to have an (optional) distinct name
     *                     - could be empty string when not specified
     * @param beanName - The EJB name which by default is the simple class name of the bean implementation class
     * @param viewClassName - the remote view is fully qualified class name of @Remote EJB interface
     * @param isStateful - if the bean is stateful set to true
     *
     * @return - JNDI context string to use in your client JNDI lookup
     */
    public static String createRemoteEjbJndiContext(
            String appName,
            String moduleName,
            String distinctName,
            String beanName,
            String viewClassName,
            boolean isStateful) {

        return "ejb:" + appName + "/" + moduleName + "/" + distinctName + "/" + beanName + "!" + viewClassName
                + (isStateful ? "?stateful" : "");
    }

    /**
     * Helper to create InitialContext with necessary properties.
     *
     * @return new InitialContext.
     * @throws NamingException
     */
    public static Context createNamingContext() throws NamingException {

        final Properties jndiProps = new Properties();
        jndiProps.setProperty(Context.URL_PKG_PREFIXES, "org.jboss.ejb.client.naming");

        return new InitialContext(jndiProps);

    }

}
