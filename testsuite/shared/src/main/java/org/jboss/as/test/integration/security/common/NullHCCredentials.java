/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.as.test.integration.security.common;

import java.security.Principal;

import org.apache.http.auth.Credentials;

/**
 * An empty Apache HTTPClient {@link Credentials} implementation, used for SPNEGO authentications.
 *
 * @author Josef Cacek
 */
public class NullHCCredentials implements Credentials {

    // Public methods --------------------------------------------------------

    /**
     * Returns <code>null</code> as the Principal.
     *
     * @return
     * @see Credentials#getUserPrincipal()
     */
    public Principal getUserPrincipal() {
        return null;
    }

    /**
     * Returns <code>null</code> as the password.
     *
     * @return
     * @see Credentials#getPassword()
     */
    public String getPassword() {
        return null;
    }
}
