/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
package org.jboss.as.test.integration.security.common.config.realm;

import java.io.UnsupportedEncodingException;

import org.jboss.util.Base64;

/**
 * A helper class to provide settings for SecurityRealm's server-identity.
 * 
 * @author Josef Cacek
 */
public class ServerIdentity {

    //Configuration of the secret/password-based identity of a server or host controller.
    private final String secret;

    // Constructors ----------------------------------------------------------

    private ServerIdentity(Builder builder) {
        this.secret = builder.secret;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the secret.
     * 
     * @return the secret.
     */
    public String getSecret() {
        return secret;
    }

    @Override
    public String toString() {
        return "ServerIdentity [secret=" + secret + "]";
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String secret;

        public Builder secretBase64(String base64secret) {
            this.secret = base64secret;
            return this;
        }

        public Builder secretPlain(String plainSecret) {
            if (plainSecret == null) {
                this.secret = null;
                return this;
            }

            byte[] secretBytes;
            try {
                secretBytes = plainSecret.getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                secretBytes = plainSecret.getBytes();
            }
            this.secret = Base64.encodeBytes(secretBytes, Base64.DONT_BREAK_LINES);
            return this;
        }

        public ServerIdentity build() {
            return new ServerIdentity(this);
        }
    }

}
