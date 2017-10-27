/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat Middleware LLC, and individual contributors
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
package org.wildfly.test.manual.elytron.seccontext;

import java.io.Serializable;

/**
 * Class which hold information about calling another {@link Entry} bean or {@link WhoAmI} bean.
 *
 * @author olukas
 */
public final class CallAnotherBeanInfo implements Serializable {

    private final String username;
    private final String password;
    private final ReAuthnType type;
    private final String providerUrl;
    private final Boolean statefullWhoAmI;
    private final String lookupEjbAppName;
    private final String authzName;

    private CallAnotherBeanInfo(Builder builder) {
        this.username = builder.username;
        this.password = builder.password;
        this.type = builder.type;
        this.providerUrl = builder.providerUrl;
        this.statefullWhoAmI = builder.statefullWhoAmI;
        this.lookupEjbAppName = builder.lookupEjbAppName;
        this.authzName = builder.authzName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public ReAuthnType getType() {
        return type;
    }

    public String getProviderUrl() {
        return providerUrl;
    }

    public Boolean isStatefullWhoAmI() {
        return statefullWhoAmI;
    }

    public String getLookupEjbAppName() {
        return lookupEjbAppName;
    }

    public String getAuthzName() {
        return authzName;
    }

    public static final class Builder {

        private String username;
        private String password;
        private ReAuthnType type;
        private String providerUrl;
        private boolean statefullWhoAmI;
        private String lookupEjbAppName;
        private String authzName;

        public Builder username(String username) {
            this.username = username;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder type(ReAuthnType type) {
            this.type = type;
            return this;
        }

        public Builder providerUrl(String providerUrl) {
            this.providerUrl = providerUrl;
            return this;
        }

        public Builder statefullWhoAmI(Boolean statefullWhoAmI) {
            this.statefullWhoAmI = statefullWhoAmI;
            return this;
        }

        public Builder lookupEjbAppName(String lookupEjbAppName) {
            this.lookupEjbAppName = lookupEjbAppName;
            return this;
        }

        public Builder authzName(String authz) {
            this.authzName = authz;
            return this;
        }

        public CallAnotherBeanInfo build() {
            return new CallAnotherBeanInfo(this);
        }
    }

}
