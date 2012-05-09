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
package org.jboss.as.test.integration.security.common.config;

import java.net.URL;

/**
 * A simple config holder for JSSE keystore or truststore configuration in a security domain.
 * 
 * @author Josef Cacek
 */
public class SecureStore {

    private final URL url;
    private final String password;
    private final String type;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new SecureStore.
     * 
     * @param builder
     */
    private SecureStore(Builder builder) {
        this.url = builder.url;
        this.password = builder.password;
        this.type = builder.type;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the url.
     * 
     * @return the url.
     */
    public URL getUrl() {
        return url;
    }

    /**
     * Get the password.
     * 
     * @return the password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Get the type.
     * 
     * @return the type.
     */
    public String getType() {
        return type;
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private URL url;
        private String password;
        private String type;

        public Builder url(URL url) {
            this.url = url;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder type(String type) {
            this.type = type;
            return this;
        }

        public SecureStore build() {
            return new SecureStore(this);
        }
    }

}
