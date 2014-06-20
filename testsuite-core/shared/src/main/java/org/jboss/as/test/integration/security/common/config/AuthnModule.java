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

import java.util.HashMap;
import java.util.Map;

/**
 * A simple config holder for authn-module configuration of JASPIC authentication in a security domain.
 *
 * @author Josef Cacek
 */
public class AuthnModule {

    private final String name;
    private final String flag;
    private final Map<String, String> options;
    private final String loginModuleStackRef;
    private final String module;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new JaspiAuthnModule.
     *
     * @param builder
     */
    private AuthnModule(Builder builder) {
        this.name = builder.name;
        this.flag = builder.flag;
        this.options = builder.options;
        this.loginModuleStackRef = builder.loginModuleStackRef;
        this.module = builder.module;
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the options.
     *
     * @return the options.
     */
    public Map<String, String> getOptions() {
        return options;
    }

    /**
     * Get the module.
     *
     * @return the module.
     */
    public String getModule() {
        return module;
    }

    /**
     * Get the name.
     *
     * @return the name.
     */
    public String getName() {
        return name;
    }

    /**
     * Get the flag.
     *
     * @return the flag.
     */
    public String getFlag() {
        return flag;
    }

    /**
     * Get the loginModuleStackRef.
     *
     * @return the loginModuleStackRef.
     */
    public String getLoginModuleStackRef() {
        return loginModuleStackRef;
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String name;
        private String flag;
        private Map<String, String> options;
        private String loginModuleStackRef;
        private String module;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder flag(String flag) {
            this.flag = flag;
            return this;
        }

        public Builder options(Map<String, String> options) {
            this.options = options;
            return this;
        }

        /**
         * Adds a single option to the options Map.
         *
         * @param name
         * @param value
         * @return
         */
        public Builder putOption(String name, String value) {
            if (options == null) {
                options = new HashMap<String, String>();
            }
            options.put(name, value);
            return this;
        }

        public Builder loginModuleStackRef(String loginModuleStackRef) {
            this.loginModuleStackRef = loginModuleStackRef;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public AuthnModule build() {
            return new AuthnModule(this);
        }
    }

}
