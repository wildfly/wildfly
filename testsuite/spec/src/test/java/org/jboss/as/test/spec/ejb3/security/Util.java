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
package org.jboss.as.test.spec.ejb3.security;

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

}
