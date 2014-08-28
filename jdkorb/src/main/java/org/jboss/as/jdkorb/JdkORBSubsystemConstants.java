/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jdkorb;

/**
 * <p>
 * Collection of constants used in the JdkORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public final class JdkORBSubsystemConstants {

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private JdkORBSubsystemConstants() {
    }

    // subsystem configuration constants (elements and attributes).

    public static final String CLIENT = "client";
    public static final String ORB = "orb";
    public static final String ORB_GIOP_VERSION = "giop-version";
    public static final String ORB_TCP = "tcp";
    public static final String TCP_HIGH_WATER_MARK = "high-water-mark";
    public static final String TCP_NUMBER_TO_RECLAIM = "number-to-reclaim";
    public static final String ORB_SOCKET_BINDING = "socket-binding";
    public static final String ORB_SSL_SOCKET_BINDING = "ssl-socket-binding";
    public static final String ORB_PERSISTENT_SERVER_ID = "persistent-server-id";
    public static final String ORB_INIT = "initializers";
    public static final String ORB_INIT_SECURITY = "security";
    public static final String ORB_INIT_TRANSACTIONS = "transactions";
    public static final String NAMING = "naming";
    public static final String NAMING_EXPORT_CORBALOC = "export-corbaloc";
    public static final String NAMING_ROOT_CONTEXT = "root-context";
    public static final String IDENTITY = "identity";
    public static final String SECURITY = "security";
    public static final String SECURITY_SUPPORT_SSL = "support-ssl";
    public static final String SECURITY_SECURITY_DOMAIN = "security-domain";
    public static final String SECURITY_ADD_COMP_VIA_INTERCEPTOR = "add-component-via-interceptor";
    public static final String SECURITY_CLIENT_SUPPORTS = "client-supports";
    public static final String SECURITY_CLIENT_REQUIRES = "client-requires";
    public static final String SECURITY_SERVER_SUPPORTS = "server-supports";
    public static final String SECURITY_SERVER_REQUIRES = "server-requires";
    public static final String SECURITY_USE_DOMAIN_SF = "use-domain-socket-factory";
    public static final String SECURITY_USE_DOMAIN_SSF = "use-domain-server-socket-factory";
    public static final String PROPERTIES = "properties";
    public static final String PROPERTY = "property";
    public static final String PROPERTY_NAME = "name";
    public static final String PROPERTY_VALUE = "value";

    public static final String SETTING = "setting";
    public static final String DEFAULT = "default";
    public static final String IOR_SETTINGS = "ior-settings";
    public static final String IOR_TRANSPORT_CONFIG = "transport-config";
    public static final String IOR_TRANSPORT_INTEGRITY = "integrity";
    public static final String IOR_TRANSPORT_CONFIDENTIALITY = "confidentiality";
    public static final String IOR_TRANSPORT_TRUST_IN_TARGET = "trust-in-target";
    public static final String IOR_TRANSPORT_TRUST_IN_CLIENT = "trust-in-client";
    public static final String IOR_TRANSPORT_DETECT_REPLAY = "detect-replay";
    public static final String IOR_TRANSPORT_DETECT_MISORDERING = "detect-misordering";
    public static final String IOR_AS_CONTEXT = "as-context";
    public static final String IOR_AS_CONTEXT_AUTH_METHOD = "auth-method";
    public static final String IOR_AS_CONTEXT_REALM = "realm";
    public static final String IOR_AS_CONTEXT_REQUIRED = "required";
    public static final String IOR_SAS_CONTEXT = "sas-context";
    public static final String IOR_SAS_CONTEXT_CALLER_PROPAGATION = "caller-propagation";
    public static final String CLIENT_TRANSPORT_CONFIG = "client-transport";
    public static final String CLIENT_TRANSPORT_REQUIRES_SSL = "requires-ssl";

    // constants for common org.omg properties.
    public static final String ORB_ADDRESS = "OAIAddr";
    public static final String ORB_PORT = "OAPort";
    public static final String ORB_SSL_PORT = "OASSLPort";
    public static final String ORB_CLASS = "org.omg.CORBA.ORBClass";
    public static final String ORB_SINGLETON_CLASS = "org.omg.CORBA.ORBSingletonClass";
    public static final String ORB_INITIALIZER_PREFIX = "org.omg.PortableInterceptor.ORBInitializerClass.";

    // JdkORB implementation classes and standard interceptors.
    public static final String JDKORB_SSL_SOCKET_FACTORY = "jdkorb.ssl.socket_factory";
    public static final String JDKORB_SSL_SERVER_SOCKET_FACTORY = "jdkorb.ssl.server_socket_factory";

    // JdkORB configuration properties that are built and set by the ORB service.
    public static final String JDKORB_NAME_SERVICE_INIT_REF = "NameService";

    public static final String SSL_SOCKET_TYPE = "SSL";
}
