/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jacorb;

/**
 * <p>
 * Collection of constants used in the JacORB subsystem.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
public final class JacORBSubsystemConstants {

    /**
     * <p>
     * Private constructor as required by the {@code Singleton} pattern.
     * </p>
     */
    private JacORBSubsystemConstants() {
    }

    // subsystem configuration constants (elements and attributes).
    public static final String ORB = "orb";
    public static final String NAME = "name";
    public static final String ORB_PRINT_VERSION = "print-version";
    public static final String ORB_USE_IMR = "use-imr";
    public static final String ORB_USE_BOM = "use-bom";
    public static final String ORB_CACHE_TYPECODES = "cache-typecodes";
    public static final String ORB_CACHE_POA_NAMES = "cache-poa-names";
    public static final String ORB_GIOP_MINOR_VERSION = "giop-minor-version";
    public static final String ORB_CONN = "connection";
    public static final String ORB_CONN_RETRIES = "retries";
    public static final String ORB_CONN_RETRY_INTERVAL = "retry-interval";
    public static final String ORB_CONN_CLIENT_TIMEOUT = "client-timeout";
    public static final String ORB_CONN_SERVER_TIMEOUT = "server-timeout";
    public static final String ORB_CONN_MAX_SERVER_CONNECTIONS = "max-server-connections";
    public static final String ORB_CONN_MAX_MANAGED_BUF_SIZE = "max-managed-buf-size";
    public static final String ORB_CONN_OUTBUF_SIZE = "outbuf-size";
    public static final String ORB_CONN_OUTBUF_CACHE_TIMEOUT = "outbuf-cache-timeout";
    public static final String ORB_INIT = "initializers";
    public static final String ORB_INIT_SECURITY = "security";
    public static final String ORB_INIT_TRANSACTIONS = "transactions";
    public static final String POA = "poa";
    public static final String POA_MONITORING = "monitoring";
    public static final String POA_QUEUE_WAIT = "queue-wait";
    public static final String POA_QUEUE_MIN = "queue-min";
    public static final String POA_QUEUE_MAX = "queue-max";
    public static final String POA_RP = "request-processors";
    public static final String POA_RP_POOL_SIZE = "pool-size";
    public static final String POA_RP_MAX_THREADS = "max-threads";
    public static final String NAMING = "naming";
    public static final String NAMING_EXPORT_CORBALOC = "export-corbaloc";
    public static final String NAMING_ROOT_CONTEXT = "root-context";
    public static final String INTEROP = "interop";
    public static final String INTEROP_SUN = "sun";
    public static final String INTEROP_COMET = "comet";
    public static final String INTEROP_IONA = "iona";
    public static final String INTEROP_CHUNK_RMI_VALUETYPES = "chunk-custom-rmi-valuetypes";
    public static final String INTEROP_LAX_BOOLEAN_ENCODING = "lax-boolean-encoding";
    public static final String INTEROP_INDIRECTION_ENCODING_DISABLE = "indirection-encoding-disable";
    public static final String INTEROP_STRICT_CHECK_ON_TC_CREATION = "strict-check-on-tc-creation";
    public static final String SECURITY = "security";
    public static final String SECURITY_SUPPORT_SSL = "support-ssl";
    public static final String SECURITY_ADD_COMP_VIA_INTERCEPTOR = "add-component-via-interceptor";
    public static final String SECURITY_CLIENT_SUPPORTS = "client-supports";
    public static final String SECURITY_CLIENT_REQUIRES = "client-requires";
    public static final String SECURITY_SERVER_SUPPORTS = "server-supports";
    public static final String SECURITY_SERVER_REQUIRES = "server-requires";
    public static final String SECURITY_USE_DOMAIN_SF = "use-domain-socket-factory";
    public static final String SECURITY_USE_DOMAIN_SSF = "use-domain-server-socket-factory";
    public static final String PROPERTIES = "properties";
    public static final String PROPERTY = "property";
    public static final String PROPERTY_KEY = "key";
    public static final String PROPERTY_VALUE = "value";

    // constants for common org.omg properties.
    public static final String ORB_ADDRESS = "OAIAddr";
    public static final String ORB_PORT = "OAPort";
    public static final String ORB_SSL_PORT = "OASSLPort";
    public static final String ORB_CLASS = "org.omg.CORBA.ORBClass";
    public static final String ORB_SINGLETON_CLASS = "org.omg.CORBA.ORBSingletonClass";
    public static final String ORB_INITIALIZER_PREFIX = "org.omg.PortableInterceptor.ORBInitializerClass.";

    // JacORB implementation classes and standard interceptors.
    public static final String JACORB_ORB_CLASS = "org.jacorb.orb.ORB";
    public static final String JacORB_ORB_SINGLETON_CLASS = "org.jacorb.orb.ORBSingleton";
    public static final String JACORB_STD_INITIALIZER_KEY = ORB_INITIALIZER_PREFIX + "standard_init";
    public static final String JACORB_STD_INITIALIZER_VALUE = "org.jacorb.orb.standardInterceptors.IORInterceptorInitializer";

    // JacORB configuration properties that are built and set by the ORB service.
    public static final String JACORB_NAME_SERVICE_INIT_REF = "ORBInitRef.NameService";
    public static final String JACORB_NAME_SERVICE_MAP_KEY = "jacorb.orb.objectKeyMap.NameService";
}
