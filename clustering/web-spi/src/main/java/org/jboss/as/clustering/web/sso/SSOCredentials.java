/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.as.clustering.web.sso;

import java.io.Serializable;

/**
 * Authentication credentials for an SSO.
 *
 * @author Brian Stansberry
 */
public class SSOCredentials implements Serializable {
    private static final long serialVersionUID = 5704877226920571663L;

    private final String authType;
    private final String password;
    private final String username;

    /**
     * Creates a new SSOCredentials.
     *
     * @param authType The authorization method used to authorize the SSO (BASIC, CLIENT-CERT, DIGEST, FORM or NONE).
     * @param username The username of the user associated with the SSO
     * @param password The password of the user associated with the SSO
     */
    public SSOCredentials(String authType, String username, String password) {
        this.authType = authType;
        this.username = username;
        this.password = password;
    }

    /**
     * Gets the username of the user associated with the SSO.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Gets the authorization method used to authorize the SSO.
     *
     * @return "BASIC", "CLIENT-CERT", "DIGEST" or "FORM"
     */
    public String getAuthType() {
        return authType;
    }

    /**
     * Gets the password of the user associated with the SSO.
     *
     * @return the password, or <code>null</code> if the authorization type was DIGEST or CLIENT-CERT.
     */
    public String getPassword() {
        return password;
    }

}