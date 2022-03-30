/*
 * Copyright (C) 2020 RedHat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wildfly.extension.vertx;

import io.vertx.core.http.ClientAuth;
import io.vertx.core.net.KeyCertOptions;
import io.vertx.core.net.TrustOptions;
import org.jboss.as.controller.capability.RuntimeCapability;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public interface VertxConstants {

    RuntimeCapability<Void> KEY_CERT_OPTIONS_CAPABILITY =
      RuntimeCapability.Builder.of(VertxResourceDefinition.VERTX_CAPABILITY_NAME + ".key-cert.options", true, KeyCertOptions.class)
        .build();

    RuntimeCapability<Void> TRUST_OPTIONS_CAPABILITY =
      RuntimeCapability.Builder.of(VertxResourceDefinition.VERTX_CAPABILITY_NAME + ".trust.options", true, TrustOptions.class)
        .build();

    enum SSL_ENGINE_TYPE {
        JDK,
        OPENSSL
    }

    String[] SSL_ENGINE_TYPES = Arrays.stream(SSL_ENGINE_TYPE.values()).map(Enum::toString).collect(Collectors.toList()).toArray(new String[0]);
    String[] TIME_UNITS = Arrays.stream(TimeUnit.values()).map(Enum::toString).collect(Collectors.toList()).toArray(new String[0]);
    String[] CLIENT_AUTHS = Arrays.stream(ClientAuth.values()).map(Enum::toString).collect(Collectors.toList()).toArray(new String[0]);

    String DEFAULT_VERTX_OPTION_NAME = "__DEFAULT__";
    String DEFAULT_JNDI_PREFIX = "java:/vertx/";
    String KEY_STORE_TYPE_JKS = "jks";
    String KEY_STORE_TYPE_PKCS12 = "pkcs12";

    String ELEMENT_VERTXES = "vertxes";
    String ELEMENT_VERTX = "vertx";

    String ELEMENT_VERTX_OPTIONS = "vertx-options";
    String ELEMENT_VERTX_OPTIONS_FILE = "vertx-option-file";
    String ELEMENT_VERTX_OPTION = "vertx-option";
    String ELEMENT_VERTX_OPTION_ADDRESS_RESOLVER = "address-resolver-option";
    String ELEMENT_VERTX_EVENTBUS = "eventbus-option";
    String ELEMENT_CLUSTER_NODE_METADATA = "cluster-node-metadata";
    String ELEMENT_KEY_STORE = "key-store-option";
    String ELEMENT_PEM_KEY_CERT = "pem-key-cert-option";
    String ELEMENT_PEM_TRUST = "pem-trust-option";

    String ATTR_NAME = "name";
    String ATTR_JNDI_NAME = "jndi-name";
    String ATTR_OPTION_NAME = "option-name";
    String ATTR_CLUSTERED = "clustered";
    String ATTR_JGROUPS_CHANNEL = "jgroups-channel";
    String ATTR_FORKED_CHANNEL = "forked-channel";
    String ATTR_JGROUPS_STACK_FILE = "jgroups-stack-file";
    String ATTR_ALIAS = "alias";
    String ATTR_PATH = "path";

    // basic vertx-option attributes
    String ATTR_EVENTLOOP_POOL_SIZE = "event-loop-pool-size";
    String ATTR_WORKER_POOL_SIZE = "worker-pool-size";
    String ATTR_INTERNAL_BLOCKING_POOL_SIZE = "internal-blocking-pool-size";
    String ATTR_HA_ENABLED = "ha-enabled";
    String ATTR_HA_GROUP = "ha-group";
    String ATTR_QUORUM_SIZE = "quorum-size";
    String ATTR_PREFER_NATIVE_TRANSPORT = "prefer-native-transport";
    String ATTR_DISABLE_TCCL = "disable-tccl";
    String ATTR_BLOCKED_THREAD_CHECK_INTERVAL = "blocked-thread-check-interval";
    String ATTR_BLOCKED_THREAD_CHECK_INTERVAL_UNIT = "blocked-thread-check-interval-unit";
    String ATTR_MAX_EVENTLOOP_EXECUTE_TIME = "max-eventloop-execute-time";
    String ATTR_MAX_EVENTLOOP_EXECUTE_TIME_UNIT = "max-eventloop-execute-time-unit";
    String ATTR_MAX_WORKER_EXECUTE_TIME = "max-worker-execute-time";
    String ATTR_MAX_WORKER_EXECUTE_TIME_UNIT = "max-worker-execute-time-unit";
    String ATTR_WARNING_EXECUTION_TIME = "warning-exception-time";
    String ATTR_WARNING_EXECUTION_TIME_UNIT = "warning-exception-time-unit";

    // file system options
    String ATTR_FS_CLASS_PATH_RESOLVING_ENABLED = "classpath-resolving-enabled";
    String ATTR_FS_FILE_CACHE_ENABLED = "file-cache-enabled";

    // address resolver options
    String ATTR_HOSTS_PATH = "hosts-path";
    String ATTR_HOSTS_VALUE = "hosts-value";
    String ATTR_SERVERS = "servers";
    String ATTR_OPT_RES_ENABLED = "opt-resource-enabled";
    String ATTR_CACHE_MIN_TTL = "cache-min-time-to-live";
    String ATTR_MAX_TTL = "cache-max-time-to-live";
    String ATTR_NEGATIVE_TTL = "cache-negative-time-to-live";
    String ATTR_QUERY_TIMEOUT = "query-time-out";
    String ATTR_MAX_QUERIES = "max-queries";
    String ATTR_RD_FLAG = "rd-flag";
    String ATTR_SEARCH_DOMAIN = "search-domains";
    String ATTR_N_DOTS = "n-dots";
    String ATTR_ROTATE_SERVERS = "rotate-servers";
    String ATTR_ROUND_ROBIN_INET_ADDRESS = "round-robin-inet-address";

    // event bus options
    String ATTR_EVENTBUS_SEND_BUFFER_SIZE = "send-buffer-size";
    String ATTR_EVENTBUS_RECEIVE_BUFFER_SIZE = "receive-buffer-size";
    String ATTR_EVENTBUS_TRAFFIC_CLASS = "traffic-class";
    String ATTR_EVENTBUS_REUSE_ADDRESS = "reuse-address";
    String ATTR_EVENTBUS_LOG_ACTIVITY = "log-activity";
    String ATTR_EVENTBUS_REUSE_PORT = "reuse-port";
    String ATTR_EVENTBUS_TCP_NO_DELAY = "tcp-no-delay";
    String ATTR_EVENTBUS_TCP_KEEP_ALIVE = "tcp-keep-alive";
    String ATTR_EVENTBUS_SO_LINGER = "so-linger";
    String ATTR_EVENTBUS_IDLE_TIMEOUT = "idle-timeout";
    String ATTR_EVENTBUS_READ_IDLE_TIMEOUT = "read-idle-timeout";
    String ATTR_EVENTBUS_WRITE_IDLE_TIMEOUT = "write-idle-timeout";
    String ATTR_EVENTBUS_IDLE_TIMEOUT_UNIT = "idle-timeout-unit";
    String ATTR_EVENTBUS_SSL = "ssl";
    String ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT = "ssl-hand-shake-timeout";
    String ATTR_EVENTBUS_SSL_HAND_SHAKE_TIMEOUT_UNIT = "ssl-hand-shake-timeout-unit";
    String ATTR_EVENTBUS_ENABLED_CIPHER_SUITES = "enabled-cipher-suites";
    String ATTR_EVENTBUS_CRL_PATHS = "crl-paths";
    String ATTR_EVENTBUS_CRL_VALUES = "crl-values";
    String ATTR_EVENTBUS_USE_ALPN = "use-alpn";
    String ATTR_EVENTBUS_ENABLED_SECURE_TRANSPORT_PROTOCOLS = "enabled-secure-transport-protocols";
    String ATTR_EVENTBUS_TCP_FAST_OPEN = "tcp-fast-open";
    String ATTR_EVENTBUS_TCP_CORK = "tcp-cork";
    String ATTR_EVENTBUS_TCP_QUICK_ACK = "tcp-quick-ack";
    String ATTR_EVENTBUS_SSL_ENGINE_TYPE = "ssl-engine-type";
    String ATTR_EVENTBUS_OPENSSL_SESSION_CACHE_ENABLED = "openssl-session-cache-enabled";
    String ATTR_EVENTBUS_CLUSTER_PUBLIC_HOST = "cluster-public-host";
    String ATTR_EVENTBUS_CLUSTER_PUBLIC_PORT = "cluster-public-port";
    String ATTR_EVENTBUS_CLUSTER_PING_INTERVAL = "cluster-ping-interval";
    String ATTR_EVENTBUS_CLUSTER_PING_REPLY_INTERVAL = "cluster-ping-reply-interval";
    String ATTR_EVENTBUS_HOST = "host";
    String ATTR_EVENTBUS_PORT = "port";
    String ATTR_EVENTBUS_ACCEPT_BACKLOG = "accept-backlog";
    String ATTR_EVENTBUS_CLIENT_AUTH = "client-auth";
    String ATTR_EVENTBUS_RECONNECT_ATTEMPTS = "reconnect-attempts";
    String ATTR_EVENTBUS_RECONNECT_INTERVAL = "reconnect-interval";
    String ATTR_EVENTBUS_CONNECT_TIMEOUT = "connect-timeout";
    String ATTR_EVENTBUS_TRUST_ALL = "trust-all";
    String ATTR_EVENTBUS_KEY_CERT_OPTION = "key-cert-option";
    String ATTR_EVENTBUS_TRUST_OPTION = "trust-option";
    String ATTR_EVENTBUS_CLUSTER_NODE_METADATA = "cluster-node-metadata";

    String ATTR_PROPERTIES = "properties";

    // key-store-option
    String ATTR_KEYSTORE_PROVIDER = "provider";
    String ATTR_KEYSTORE_TYPE = "type";
    String ATTR_KEYSTORE_PASSWORD = "password";
    String ATTR_KEYSTORE_PATH = "path";
    String ATTR_KEYSTORE_VALUE = "value";
    String ATTR_KEYSTORE_ALIAS = "alias";
    String ATTR_KEYSTORE_ALIAS_PASSWORD = "alias-password";

    // pem-key-cert-option
    String ATTR_PEM_KEY_CERT_KEY_PATH = "key-paths";
    String ATTR_PEM_KEY_CERT_KEY_VALUE = "key-values";
    String ATTR_PEM_KEY_CERT_CERT_PATH = "cert-paths";
    String ATTR_PEM_KEY_CERT_CERT_VALUE = "cert-values";
    String ATTR_PEM_VALUE = "pem-value";

    // default infinispan.xml file from which to read the cache configurations.
    String DEFAULT_INFINISPAN_FILE = "infinispan-config.xml";
}
