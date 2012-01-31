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

package org.jboss.as.jaxr;

import java.util.HashMap;
import java.util.Map;


/**
 * Constants used by the JAXR subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 07-Nov-2011
 */
public interface JAXRConstants {

    //JAXR Property Names
    String JAXR_FACTORY_IMPLEMENTATION  = "javax.xml.registry.ConnectionFactory";
    String QUERYMANAGER                 = "javax.xml.registry.queryManagerURL";
    String LIFECYCLEMANAGER             = "javax.xml.registry.lifeCycleManagerURL";
    String SECURITYMANAGER              = "javax.xml.registry.securityManagerURL";

    // DEFAULT VALUES
    String DEFAULT_JAXR_FACTORY_IMPL    = "org.apache.ws.scout.registry.ConnectionFactoryImpl";
    String DEFAULT_QUERYMANAGER         = "http://localhost:8080/juddi/inquiry";
    String DEFAULT_LIFECYCLEMANAGER     = "http://localhost:8080/juddi/publish";
    String DEFAULT_V3_QUERYMANAGER      = "http://localhost:8080/juddiv3/inquiry";
    String DEFAULT_V3_LIFECYCLEMANAGER  = "http://localhost:8080/juddiv3/publish";
    String DEFAULT_V3_SECURITYMANAGER   = "http://localhost:8080/juddiv3/security";

    //Scout Property Names
    String UDDI_VERSION_PROPERTY_NAME   = "scout.proxy.uddiVersion";
    String UDDI_NAMESPACE_PROPERTY_NAME = "scout.proxy.uddiNamespace";
    String SCOUT_TRANSPORT              = "scout.proxy.transportClass";
    String UDDI_V2_VERSION              = "2.0";
    String UDDI_V3_VERSION              = "3.0";
    String UDDI_V2_NAMESPACE            = "urn:uddi-org:api_v2";
    String UDDI_V3_NAMESPACE            = "urn:uddi-org:api_v3";
    String SCOUT_SAAJ_TRANSPORT         =  "org.jboss.as.jaxr.scout.SaajTransport";
    String SCOUT_LOCAL_TRANSPORT        =  "org.apache.ws.scout.transport.LocalTransport";

    //Module properties
    String SUBSYSTEM_NAME               = "jaxr";
    String RESOURCE_NAME                = "org.jboss.as.jaxr.LocalDescriptions";

    enum Namespace {

        // must be first
        UNKNOWN(null),
        JAXR_1_0("urn:jboss:domain:jaxr:1.0");

        public static final Namespace CURRENT = JAXR_1_0;

        private final String name;
        private static final Map<String, Namespace> MAP;

        static {
            final Map<String, Namespace> map = new HashMap<String, Namespace>();
            for (Namespace namespace : values()) {
                final String name = namespace.getUriString();
                if (name != null) map.put(name, namespace);
            }
            MAP = map;
        }

        private Namespace(final String name) {
            this.name = name;
        }

        public String getUriString() {
            return name;
        }

        public static Namespace forUri(String uri) {
            final Namespace element = MAP.get(uri);
            return element == null ? UNKNOWN : element;
        }
    }

    enum Attribute {
        UNKNOWN(null),
        JNDI_NAME("jndi-name"),
        CLASS("class"),
        NAME("name"),
        VALUE("value");

        private final String name;
        private static final Map<String, Attribute> MAP;

        static {
            final Map<String, Attribute> map = new HashMap<String, Attribute>();
            for (Attribute element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        private Attribute(final String name) {
            this.name = name;
        }

        public String getLocalName() {
            return name;
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
        CONNECTION_FACTORY("connection-factory"),
        PROPERTIES("properties"),
        PROPERTY("property");

        private final String name;
        private static final Map<String, Element> MAP;

        static {
            final Map<String, Element> map = new HashMap<String, Element>();
            for (Element element : values()) {
                final String name = element.getLocalName();
                if (name != null) map.put(name, element);
            }
            MAP = map;
        }

        private Element(final String name) {
            this.name = name;
        }

        public String getLocalName() {
            return name;
        }

        public static Element forName(String localName) {
            final Element element = MAP.get(localName);
            return element == null ? UNKNOWN : element;
        }
    }
}
