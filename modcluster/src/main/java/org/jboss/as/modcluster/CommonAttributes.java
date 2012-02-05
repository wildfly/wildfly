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
package org.jboss.as.modcluster;

/**
 * @author Jean-Frederic Clere
 * @author Radoslav Husar
 */
interface CommonAttributes {

    String MOD_CLUSTER_CONFIG = "mod-cluster-config";
    String PROXY_CONF = "proxy-conf";
    String HTTPD_CONF = "httpd-conf";
    String NODES_CONF = "nodes-conf";
    String ADVERTISE_SOCKET = "advertise-socket";
    String SSL = "ssl";
    String PROXY_LIST = "proxy-list";
    String PROXY_URL = "proxy-url";
    String ADVERTISE = "advertise";
    String ADVERTISE_SECURITY_KEY = "advertise-security-key";
    String EXCLUDED_CONTEXTS = "excluded-contexts";
    String AUTO_ENABLE_CONTEXTS = "auto-enable-contexts";
    String STOP_CONTEXT_TIMEOUT = "stop-context-timeout";
    String SOCKET_TIMEOUT = "socket-timeout";

    String STICKY_SESSION = "sticky-session";
    String STICKY_SESSION_REMOVE = "sticky-session-remove";
    String STICKY_SESSION_FORCE = "sticky-session-force";
    String WORKER_TIMEOUT = "worker-timeout";
    String MAX_ATTEMPTS = "max-attemps";
    String FLUSH_PACKETS = "flush-packets";
    String FLUSH_WAIT = "flush-wait";
    String PING = "ping";
    String SMAX = "smax";
    String TTL = "ttl";
    String NODE_TIMEOUT = "node-timeout";
    String BALANCER = "balancer";
    String DOMAIN = "domain";

    String LOAD_METRIC = "load-metric";
    String FACTOR = "factor";
    String HISTORY = "history";
    String DECAY = "decay";
    String NAME = "name";
    String CAPACITY = "capacity";
    String TYPE = "type";
    String LOAD_PROVIDER = "load-provider";
    String SIMPLE_LOAD_PROVIDER = "simple-load-provider";
    String DYNAMIC_LOAD_PROVIDER = "dynamic-load-provider";
    String CUSTOM_LOAD_METRIC = "custom-load-metric";
    String WEIGHT = "weight";
    String CLASS = "class";
    String PROPERTY = "property";
    String VALUE = "value";
    String KEY_ALIAS = "key-alias";
    String PASSWORD = "password";
    String CERTIFICATE_KEY_FILE = "certificate-key-file";
    String CIPHER_SUITE = "cipher-suite";
    String PROTOCOL = "protocol";
    String VERIFY_CLIENT = "verify-client";
    String CA_CERTIFICATE_FILE = "ca-certificate-file";
    String CA_REVOCATION_URL = "ca-revocation-url";
    String CONFIGURATION = "configuration";

    /**
     * @since v1.1
     */
    String ENABLE = "enable";
    String FORCE = "force";
    String REMOVE = "remove";
    String PROXIES = "proxies";
    String URL = "url";
    String CONTEXTS = "contexts";
    String SOCKET_BINDING = "socket-binding";
    String SECURITY_KEY = "security-key";
    String OUTBOUT_SOCKET_BINDINGS = "outbound-socket-bindings";
    String LOAD_BALANCING_GROUP = "load-balancing-group";
    String AUTO_ENABLE = "auto-enable";
    String STOP_TIMEOUT = "stop-timeout";
}
