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

/**
 * A helper class to provide settings for security realms.
 *
 * <pre>
 * /core-service=management/security-realm=ApplicationRealm:read-resource-description()
 * </pre>
 *
 * @author Josef Cacek
 */
public class SecurityRealm {

    private final String name;
    private final ServerIdentity serverIdentity;
    private final Authentication authentication;
    private final Authorization authorization;

    //    private final RealmAuthentication;

    // Constructors ----------------------------------------------------------

    private SecurityRealm(Builder builder) {
        this.name = builder.name;
        this.serverIdentity = builder.serverIdentity;
        this.authentication = builder.authentication;
        this.authorization = builder.authorization;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the serverIdentity.
     *
     * @return the serverIdentity.
     */
    public ServerIdentity getServerIdentity() {
        return serverIdentity;
    }

    /**
     * Get the authentication.
     *
     * @return the authentication.
     */
    public Authentication getAuthentication() {
        return authentication;
    }

    /**
     * Get the authorization.
     *
     * @return the authorization.
     */
    public Authorization getAuthorization() {
        return authorization;
    }

    @Override
    public String toString() {
        return "SecurityRealm [name=" + name + ", serverIdentity=" + serverIdentity + ", authentication=" + authentication
                + ", authorization=" + authorization + "]";
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String name;
        private ServerIdentity serverIdentity;
        private Authentication authentication;
        private Authorization authorization;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder serverIdentity(ServerIdentity serverIdentity) {
            this.serverIdentity = serverIdentity;
            return this;
        }

        public Builder authentication(Authentication authentication) {
            this.authentication = authentication;
            return this;
        }

        public Builder authorization(Authorization authorization) {
            this.authorization = authorization;
            return this;
        }

        public SecurityRealm build() {
            return new SecurityRealm(this);
        }
    }

}
