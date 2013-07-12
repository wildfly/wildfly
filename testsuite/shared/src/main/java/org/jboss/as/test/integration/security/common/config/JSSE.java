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


/**
 * A simple config holder for JSSE configuration in a security domain.
 *
 * @author Josef Cacek
 */
public class JSSE {

    private final SecureStore keyStore;
    private final SecureStore trustStore;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new JSSE.
     *
     * @param builder
     */
    private JSSE(Builder builder) {
        this.keyStore = builder.keyStore;
        this.trustStore = builder.trustStore;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the keyStore.
     *
     * @return the keyStore.
     */
    public SecureStore getKeyStore() {
        return keyStore;
    }

    /**
     * Get the trustStore.
     *
     * @return the trustStore.
     */
    public SecureStore getTrustStore() {
        return trustStore;
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private SecureStore keyStore;
        private SecureStore trustStore;

        public Builder keyStore(SecureStore keyStore) {
            this.keyStore = keyStore;
            return this;
        }

        public Builder trustStore(SecureStore trustStore) {
            this.trustStore = trustStore;
            return this;
        }

        public JSSE build() {
            return new JSSE(this);
        }
    }

}
