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
 * A simple config holder for loginModuleStack configuration of JASPIC authentication in a security domain.
 * 
 * @author Josef Cacek
 */
public class LoginModuleStack {

    private final String name;
    private final SecurityModule[] loginModules;

    // Constructors ----------------------------------------------------------

    private LoginModuleStack(Builder builder) {
        this.name = builder.name;
        this.loginModules = builder.loginModules;
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
     * Get the loginModules.
     * 
     * @return the loginModules.
     */
    public SecurityModule[] getLoginModules() {
        return loginModules;
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String name;
        private SecurityModule[] loginModules;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder loginModules(SecurityModule... loginModules) {
            this.loginModules = loginModules;
            return this;
        }

        public LoginModuleStack build() {
            return new LoginModuleStack(this);
        }
    }

}
