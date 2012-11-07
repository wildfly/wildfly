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

package org.jboss.as.jacorb;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface PropertiesMap {

    Map<String, String> JACORB_PROPS_MAP = Collections.unmodifiableMap(new HashMap<String, String>() {

        {
            put(JacORBSubsystemConstants.NAME, "jacorb.implname");
            put(JacORBSubsystemConstants.ORB_PRINT_VERSION, "jacorb.orb.print_version");
            put(JacORBSubsystemConstants.ORB_USE_IMR, "jacorb.use_imr");
            put(JacORBSubsystemConstants.ORB_USE_BOM, "jacorb.use_bom");
            put(JacORBSubsystemConstants.ORB_CACHE_TYPECODES, "jacorb.cacheTypecodes");
            put(JacORBSubsystemConstants.ORB_CACHE_POA_NAMES, "jacorb.cachePoaNames");
            put(JacORBSubsystemConstants.ORB_GIOP_MINOR_VERSION, "jacorb.giop.minor_version");
            put(JacORBSubsystemConstants.ORB_CONN_RETRIES, "jacorb.retries");
            put(JacORBSubsystemConstants.ORB_CONN_RETRY_INTERVAL, "jacorb.retry_interval");
            put(JacORBSubsystemConstants.ORB_CONN_CLIENT_TIMEOUT, "jacorb.connection.client.idle_timeout");
            put(JacORBSubsystemConstants.ORB_CONN_SERVER_TIMEOUT, "jacorb.connection.server.timeout");
            put(JacORBSubsystemConstants.ORB_CONN_MAX_SERVER_CONNECTIONS, "jacorb.connection.max_server_connections");
            put(JacORBSubsystemConstants.ORB_CONN_MAX_MANAGED_BUF_SIZE, "jacorb.maxManagedBufSize");
            put(JacORBSubsystemConstants.ORB_CONN_OUTBUF_SIZE, "jacorb.outbuf_size");
            put(JacORBSubsystemConstants.ORB_CONN_OUTBUF_CACHE_TIMEOUT, "jacorb.bufferManagerMaxFlush");
            put(JacORBSubsystemConstants.POA_MONITORING, "jacorb.poa.monitoring");
            put(JacORBSubsystemConstants.POA_QUEUE_WAIT, "jacorb.poa.queue_wait");
            put(JacORBSubsystemConstants.POA_QUEUE_MIN, "jacorb.poa.queue_min");
            put(JacORBSubsystemConstants.POA_QUEUE_MAX, "jacorb.poa.queue_max");
            put(JacORBSubsystemConstants.POA_RP_POOL_SIZE, "jacorb.poa.thread_pool_min");
            put(JacORBSubsystemConstants.POA_RP_MAX_THREADS, "jacorb.poa.thread_pool_max");
            put(JacORBSubsystemConstants.INTEROP_SUN, "jacorb.interop.sun");
            put(JacORBSubsystemConstants.INTEROP_COMET, "jacorb.interop.comet");
            put(JacORBSubsystemConstants.INTEROP_CHUNK_RMI_VALUETYPES, "jacorb.interop.chunk_custom_rmi_valuetypes");
            put(JacORBSubsystemConstants.INTEROP_LAX_BOOLEAN_ENCODING, "jacorb.interop.lax_boolean_encoding");
            put(JacORBSubsystemConstants.INTEROP_INDIRECTION_ENCODING_DISABLE, "jacorb.interop.indirection_encoding_disable");
            put(JacORBSubsystemConstants.INTEROP_STRICT_CHECK_ON_TC_CREATION, "jacorb.interop.strict_check_on_tc_creation");
            put(JacORBSubsystemConstants.SECURITY_SUPPORT_SSL, "jacorb.security.support_ssl");
            put(JacORBSubsystemConstants.SECURITY_ADD_COMP_VIA_INTERCEPTOR, "jacorb.security.ssl_components_added_by_ior_interceptor");
            put(JacORBSubsystemConstants.SECURITY_CLIENT_SUPPORTS, "jacorb.security.ssl.client.supported_options");
            put(JacORBSubsystemConstants.SECURITY_CLIENT_REQUIRES, "jacorb.security.ssl.client.required_options");
            put(JacORBSubsystemConstants.SECURITY_SERVER_SUPPORTS, "jacorb.security.ssl.server.supported_options");
            put(JacORBSubsystemConstants.SECURITY_SERVER_REQUIRES, "jacorb.security.ssl.server.required_options");
        }
    });
}
