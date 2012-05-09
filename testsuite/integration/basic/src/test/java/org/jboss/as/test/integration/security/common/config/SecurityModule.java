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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A helper class to provide setting for the {@link SecurityDomain}.
 * 
 * @author Josef Cacek
 */
public class SecurityModule {

    private final String name;
    private final String flag;
    private final Map<String, String> options;

    // Constructors ----------------------------------------------------------

    /**
     * Create a new SecurityModule.
     * 
     * @param builder
     */
    private SecurityModule(Builder builder) {
        this.name = builder.name;
        this.flag = builder.flag;
        this.options = builder.options == null ? null : Collections
                .unmodifiableMap(new HashMap<String, String>(builder.options));
    }

    // Public methods --------------------------------------------------------

    /**
     * Get the name.
     * 
     * @return the name.
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the flag.
     * 
     * @return the flag.
     */
    public final String getFlag() {
        return flag;
    }

    /**
     * Get the options.
     * 
     * @return the options.
     */
    public final Map<String, String> getOptions() {
        return options;
    }

    // Embedded classes ------------------------------------------------------

    public static class Builder {
        private String name;
        private String flag;
        private Map<String, String> options;

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

        public Builder putOption(String name, String value) {
            if (options == null) {
                options = new HashMap<String, String>();
            }
            options.put(name, value);
            return this;
        }

        public SecurityModule build() {
            return new SecurityModule(this);
        }
    }

}
