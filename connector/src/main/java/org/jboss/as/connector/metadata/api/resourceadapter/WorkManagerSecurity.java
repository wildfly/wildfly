/*
 * Copyright 2017 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.as.connector.metadata.api.resourceadapter;

import java.util.HashMap;
import java.util.Map;

/**
 * Extension of {@link org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity} with added Elytron support.
 *
 * @author Flavia Rainone
 */
public interface WorkManagerSecurity extends org.jboss.jca.common.api.metadata.resourceadapter.WorkManagerSecurity {

    /**
     * Indicates if Elytron is enabled. In this case, {@link #getDomain()}, refers to an Elytron authentication context
     *
     * @return {@code true} if is Elytron enabled
     */
    boolean isElytronEnabled();

    /**
     * A Tag.
     *
     */
    enum Tag {
        // Elytron Tags
        /**
         * Is Elytron enabled
         */
        ELYTRON_SECURITY_DOMAIN("elytron-security-domain"),

        /* Tags copied from super class */
        /** always first
         *
         */
        UNKNOWN(null),

        /**
         * mapping-required tag
         */
        MAPPING_REQUIRED("mapping-required"),

        /**
         * domain tag
         */
        DOMAIN("domain"),

        /**
         * default-principal tag
         */
        DEFAULT_PRINCIPAL("default-principal"),

        /**
         * default-groups tag
         */
        DEFAULT_GROUPS("default-groups"),

        /**
         * group tag
         */
        GROUP("group"),

        /**
         * mappings tag
         */
        MAPPINGS("mappings"),

        /**
         * users tag
         */
        USERS("users"),

        /**
         * groups tag
         */
        GROUPS("groups"),

        /**
         * map tag
         */
        MAP("map");

        private String name;

        /**
         *
         * Create a new Tag.
         *
         * @param name a name
         */
        Tag(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this element.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        /**
         * {@inheritDoc}
         */
        public String toString() {

            return name;
        }

        private static final Map<String, Tag> MAPE;

        static {
            final Map<String, Tag> map = new HashMap<>();
            for (Tag element : values()) {
                final String name = element.getLocalName();
                if (name != null)
                    map.put(name, element);
            }
            MAPE = map;
        }

        /**
         * Set the value
         * @param v The name
         * @return The value
         */
        Tag value(String v) {
            name = v;
            return this;
        }

        /**
         *
         * Static method to get enum instance given localName string
         *
         * @param localName a string used as localname (typically tag name as defined in xsd)
         * @return the enum instance
         */
        public static Tag forName(String localName) {
            final Tag element = MAPE.get(localName);
            return element == null ? UNKNOWN.value(localName) : element;
        }
    }
}
