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

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Enumeration of the JacORB subsystem XML configuration attributes. Each member also contains the JacORB-specific name
 * of the attribute, so it is possible to use this enumeration to translate the attributes configured in the JacORB
 * subsystem to actual JacORB attributes.
 * </p>
 *
 * @author <a href="mailto:sguilhen@redhat.com">Stefan Guilhen</a>
 */
enum JacORBAttribute {

    UNKNOWN(null, null),

    // attributes of the orb element.
    ORB_NAME("name", "jacorb.implname"),
    ORB_PRINT_VERSION("print-version", "jacorb.orb.print_version"),
    ORB_USE_IMR("use-imr", "jacorb.use_imr"),
    ORB_USE_BOM("use-bom", "jacorb.use_bom"),
    ORB_CACHE_TYPECODES("cache-typecodes", "jacorb.cacheTypecodes"),
    ORB_CACHE_POA_NAMES("cache-poa-names", "jacorb.cachePoaNames"),
    ORB_GIOP_MINOR_VERSION("giop-minor-version", "jacorb.giop_minor_version"),

    // attributes of the connection element.
    ORB_CONN_RETRIES("retries", "jacorb.retries"),
    ORB_CONN_RETRY_INTERVAL("retry-interval", "jacorb.retry_interval"),
    ORB_CONN_CLIENT_TIMEOUT("client-timeout", "jacorb.connection.client.idle_timeout"),
    ORB_CONN_SERVER_TIMEOUT("server-timeout", "jacorb.connection.server.timeout"),
    ORB_CONN_MAX_SERVER_CONNECTIONS("max-server-connections", "jacorb.connection.max_server_connections"),
    ORB_CONN_MAX_MANAGED_BUF_SIZE("max-managed-buf-size", "jacorb.maxManagedBufSize"),
    ORB_CONN_OUTBUF_SIZE("outbuf-size", "jacorb.outbuf_size"),
    ORB_CONN_OUTBUF_CACHE_TIMEOUT("outbuf-cache-timeout", "jacorb.bufferManagerMaxFlush"),

    // attributes of the naming element - the ORB service will build the relevant JacORB properties from these values.
    ORB_NAMING_EXPORT_CORBALOC("export-corbaloc", null),
    ORB_NAMING_ROOT_CONTEXT("root-context", null),

    // attributes of the poa element.
    POA_MONITORING("monitoring", "jacorb.poa.monitoring"),
    POA_QUEUE_WAIT("queue-wait", "jacorb.poa.queue_wait"),
    POA_QUEUE_MIN("queue-min", "jacorb.poa.queue_min"),
    POA_QUEUE_MAX("queue-max", "jacorb.poa.queue.max"),

    // attributes of the request-processor element.
    POA_REQUEST_PROC_POOL_SIZE("pool-size", "jacorb.poa.thread_pool_min"),
    POA_REQUEST_PROC_MAX_THREADS("max-threads", "jacorb.poa.thread_pool_max"),

    // attributes of the interop element.
    INTEROP_SUN("sun", "jacorb.interop.sun"),
    INTEROP_COMET("comet", "jacorb.interop.comet"),
    INTEROP_CHUNK_RMI_VALUETYPES("chunk-custom-rmi-valuetypes", "jacorb.interop.chunk_custom_rmi_valuetypes"),
    INTEROP_LAX_BOOLEAN_ENCODING("lax-boolean-encoding", "jacorb.interop.lax_boolean_encoding"),
    INTEROP_INDIRECTION_ENCODING_DISABLE("indirection-encoding-disable", "jacorb.interop.indirection_encoding_disable"),
    INTEROP_STRICT_CHECK_ON_TC_CREATION("strict-check-on-tc-creation", "jacorb.interop.strict_check_on_tc_creation"),

    // attributes of the security element.
    SECURITY_SUPPORT_SSL("support-ssl", "jacorb.security.support_ssl"),
    SECURITY_ADD_COMPONENT_INTERCEPTOR("add-component-via-interceptor", "jacorb.security.ssl_components_added_by_ior_interceptor"),
    SECURITY_CLIENT_SUPPORTS("client-supports", "jacorb.security.ssl.client.supported_options"),
    SECURITY_CLIENT_REQUIRES("client-requires", "jacorb.security.ssl.client.required_options"),
    SECURITY_SERVER_SUPPORTS("server-supports", "jacorb.security.ssl.server.supported_options"),
    SECURITY_SERVER_REQUIRES("server-requires", "jacorb.security.ssl.server.required_options"),
    // if enabled the ORB service will configure JacORB to use the JBoss SSL socket factory classes by building the
    // appropriate properties.
    SECURITY_USE_DOMAIN_SF("use-domain-socket-factory", null),
    SECURITY_USE_DOMAIN_SSF("use-domain-server-socket-factory", null),

    // attributes of the generic property element.
    PROP_KEY("key", null),
    PROP_VALUE("value", null);

    private final String name;

    private final String jacorbName;

    /**
     * <p>
     * {@code JacORBAttribute} constructor. Sets the attribute name.
     * </p>
     *
     * @param name a {@code String} representing the local name of the attribute.
     * @param jacORBName a {@code String} that represents the JacORB configuration that corresponds to the attribute.
     */
    JacORBAttribute(final String name, final String jacORBName) {
        this.name = name;
        this.jacorbName = jacORBName;
    }

    /**
     * <p>
     * Obtains the local name of this attribute.
     * </p>
     *
     * @return a {@code String} representing the attribute local name.
     */
    public String getLocalName() {
        return this.name;
    }

    public String getJacORBName() {
        return this.jacorbName;
    }

    // a map that caches all available attributes by name.
    private static final Map<String, JacORBAttribute> MAP;

    static {
        final Map<String, JacORBAttribute> map = new HashMap<String, JacORBAttribute>();
        for (JacORBAttribute attribute : values()) {
            final String name = attribute.name;
            if (name != null)
                map.put(name, attribute);
        }
        MAP = map;
    }

    /**
     * <p>
     * Gets the {@code JacORBAttribute} identified by the specified name.
     * </p>
     *
     * @param localName a {@code String} representing the local name of the attribute.
     * @return the {@code JacORBAttribute} identified by the name. If no attribute can be found, the
     *         {@code JacORBAttribute.UNKNOWN} type is returned.
     */
    public static JacORBAttribute forName(String localName) {
        final JacORBAttribute attribute = MAP.get(localName);
        return attribute == null ? UNKNOWN : attribute;
    }
}
