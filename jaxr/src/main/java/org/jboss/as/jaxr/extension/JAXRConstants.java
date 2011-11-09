/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxr.extension;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;


/**
 * Constants used by the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @since 07-Nov-2011
 */
public interface JAXRConstants {

    String SUBSYSTEM_NAME = "jaxr";

    enum Namespace {

        // must be first
        UNKNOWN(null),
        JAXR_1_0("urn:jboss:domain:jaxr:1.0");

        static final Namespace CURRENT = JAXR_1_0;

        private final String name;

        Namespace(final String name) {
            this.name = name;
        }

        String getUriString() {
            return name;
        }

        private static final Map<String, Namespace> MAP;

        static {
            final Map<String, Namespace> map = new HashMap<String, Namespace>();
            for (Namespace namespace : values()) {
                final String name = namespace.getUriString();
                if (name != null) map.put(name, namespace);
            }
            MAP = map;
        }

        static Namespace forUri(String uri) {
            final Namespace element = MAP.get(uri);
            return element == null ? UNKNOWN : element;
        }
    }

    enum Attribute {
        UNKNOWN(null),
        JNDI_NAME("jndi-name"),
        DROPONSTART("dropOnStart"),
        CREATEONSTART("createOnStart"),
        DROPONSTOP("dropOnStop")
        ;
        private final String name;

        Attribute(final String name) {
            this.name = name;
        }

        /**
         * Get the local name of this attribute.
         *
         * @return the local name
         */
        public String getLocalName() {
            return name;
        }

        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
            for (Attribute element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Attribute forName(String localName) {
            final Attribute element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }

        public String toString() {
            return getLocalName();
        }
    }

    enum Element {
        // must be first
        UNKNOWN(null),
        CONNECTION("connection"),
        DATASOURCE("datasource"),
        FLAGS("flags"),
        ;

        private final String name;

        Element(final String name) {
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

        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }
    }
}
