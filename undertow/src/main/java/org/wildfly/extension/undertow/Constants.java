/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.undertow;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */

public interface Constants {
    String ACCESS_LOG = "access-log";
    String AJP_LISTENER = "ajp-listener";
    String BUFFER_CACHE = "buffer-cache";
    String BUFFER_CACHES = "buffer-caches";
    String BUFFER_SIZE = "buffer-size";
    String BUFFERS_PER_REGION = "buffers-per-region";
    String CONFIGURATION = "configuration";
    String MAX_REGIONS = "max-regions";
    String BUFFER_POOL = "buffer-pool";
    String SETTING = "setting";
    String SECURITY_REALM = "security-realm";
    String SOCKET_BINDING = "socket-binding";
    String SSL_CONTEXT = "ssl-context";
    String PATH = "path";
    String HTTP_LISTENER = "http-listener";
    String HTTPS_LISTENER = "https-listener";
    String HTTP_INVOKER = "http-invoker";
    String LISTENER = "listener";
    String INSTANCE_ID = "instance-id";
    String NAME = "name";
    String WORKER = "worker";
    String SERVLET_CONTAINER = "servlet-container";
    String LOCATION = "location";
    String JSP = "jsp";
    String JSP_CONFIG = "jsp-config";
    String HANDLER = "handler";
    String HANDLERS = "handlers";
    String SERVER = "server";
    String HOST = "host";
    String PATTERN = "pattern";
    String PREFIX = "prefix";
    String SUFFIX = "suffix";
    String ROTATE = "rotate";
    //String CLASS = "class";
    String DEFAULT_HOST = "default-host";
    String DEFAULT_VIRTUAL_HOST = "default-virtual-host";
    String DEFAULT_SERVLET_CONTAINER = "default-servlet-container";
    String DEFAULT_SERVER = "default-server";
    String DEFAULT_WEB_MODULE = "default-web-module";
    String ALIAS = "alias";
    String ERROR_PAGE = "error-page";
    String ERROR_PAGES = "error-pages";
    String SIMPLE_ERROR_PAGE = "simple-error-page";
    String SCHEME = "scheme";
    String MAX_POST_SIZE = "max-post-size";
    String DEFAULT_RESPONSE_CODE = "default-response-code";
    /*JSP config */
    String CHECK_INTERVAL = "check-interval";
    String CONTAINER = "container";
    String DEVELOPMENT = "development";
    String DISABLED = "disabled";
    String DISPLAY_SOURCE_FRAGMENT = "display-source-fragment";
    String DUMP_SMAP = "dump-smap";
    String ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE = "error-on-use-bean-invalid-class-attribute";
    String FILE = "file";
    String FILE_ENCODING = "file-encoding";
    String GENERATE_STRINGS_AS_CHAR_ARRAYS = "generate-strings-as-char-arrays";
    String OPTIMIZE_SCRIPTLETS = "optimize-scriptlets";
    String JAVA_ENCODING = "java-encoding";
    String JSP_CONFIGURATION = "jsp-configuration";
    String KEEP_GENERATED = "keep-generated";
    String LISTINGS = "listings";
    String MAPPED_FILE = "mapped-file";
    String MAX_DEPTH = "max-depth";
    String MIME_MAPPING = "mime-mapping";
    String MODIFICATION_TEST_INTERVAL = "modification-test-interval";
    String READ_ONLY = "read-only";
    String RECOMPILE_ON_FAIL = "recompile-on-fail";
    String SCRATCH_DIR = "scratch-dir";
    String SECRET = "secret";
    String SENDFILE = "sendfile";
    String SINGLE_SIGN_ON = "single-sign-on";
    String SMAP = "smap";
    String SOURCE_VM = "source-vm";
    String SSL = "ssl";
    String STATIC_RESOURCES = "static-resources";
    String TAG_POOLING = "tag-pooling";
    String TARGET_VM = "target-vm";
    String TRIM_SPACES = "trim-spaces";
    String WEBDAV = "webdav";
    String WELCOME_FILE = "welcome-file";
    String X_POWERED_BY = "x-powered-by";
    String ENABLED = "enabled";
    String DIRECTORY_LISTING = "directory-listing";
    String FILTER = "filter";
    String FILTERS = "filters";
    String FILTER_REF = "filter-ref";

    //session cookie config
    String SESSION_COOKIE = "session-cookie";
    String DOMAIN = "domain";
    String COMMENT = "comment";
    String HTTP_ONLY = "http-only";
    String SECURE = "secure";
    String MAX_AGE = "max-age";
    String ALLOW_NON_STANDARD_WRAPPERS = "allow-non-standard-wrappers";

    String PERSISTENT_SESSIONS = "persistent-sessions";
    String DEFAULT_BUFFER_CACHE = "default-buffer-cache";

    String RELATIVE_TO = "relative-to";
    String REDIRECT_SOCKET = "redirect-socket";
    String DIRECTORY = "directory";
    String STACK_TRACE_ON_ERROR = "stack-trace-on-error";
    String DEFAULT_ENCODING = "default-encoding";
    String USE_LISTENER_ENCODING = "use-listener-encoding";
    String NONE = "none";
    String PROBLEM_SERVER_RETRY = "problem-server-retry";
    String STICKY_SESSION_LIFETIME = "sticky-session-lifetime";
    String SESSION_COOKIE_NAMES = "session-cookie-names";
    String CONNECTIONS_PER_THREAD = "connections-per-thread";
    String REVERSE_PROXY = "reverse-proxy";
    String MAX_REQUEST_TIME = "max-request-time";
    String CERTIFICATE_FORWARDING = "certificate-forwarding";
    String OPTIONS = "options";
    String IGNORE_FLUSH = "ignore-flush";

    String WEBSOCKETS = "websockets";
    //mod_cluster
    String MOD_CLUSTER = "mod-cluster";
    String MANAGEMENT_SOCKET_BINDING = "management-socket-binding";
    String ADVERTISE_SOCKET_BINDING = "advertise-socket-binding";
    String SECURITY_KEY = "security-key";
    String ADVERTISE_PROTOCOL = "advertise-protocol";
    String ADVERTISE_PATH = "advertise-path";
    String ADVERTISE_FREQUENCY = "advertise-frequency";
    String HEALTH_CHECK_INTERVAL = "health-check-interval";
    String BROKEN_NODE_TIMEOUT = "broken-node-timeout";
    String MANAGEMENT_ACCESS_PREDICATE = "management-access-predicate";
    String REQUEST_QUEUE_SIZE = "request-queue-size";
    String CACHED_CONNECTIONS_PER_THREAD = "cached-connections-per-thread";
    String CONNECTION_IDLE_TIMEOUT = "connection-idle-timeout";
    String FAILOVER_STRATEGY = "failover-strategy";

    String USE_SERVER_LOG = "use-server-log";
    String VALUE = "value";

    String REWRITE = "rewrite";
    String DISALLOWED_METHODS = "disallowed-methods";
    String RESOLVE_PEER_ADDRESS = "resolve-peer-address";
    String BALANCER = "balancer";
    String CONTEXT = "context";
    String NODE = "node";
    String STATUS = "status";
    String REQUESTS = "requests";
    String ENABLE = "enable";
    String DISABLE = "disable";
    String LOAD = "load";
    String USE_ALIAS = "use-alias";
    String LOAD_BALANCING_GROUP = "load-balancing-group";
    String CACHE_CONNECTIONS = "cache-connections";
    String FLUSH_WAIT = "flush-wait";
    String MAX_CONNECTIONS = "max-connections";
    String OPEN_CONNECTIONS = "open-connections";
    String PING = "ping";
    String READ = "read";
    String SMAX = "smax";
    String TIMEOUT = "timeout";
    String WRITTEN = "written";
    String TTL = "ttl";

    String STICKY_SESSION = "sticky-session";
    String STICKY_SESSION_COOKIE = "sticky-session-cookie";
    String STICKY_SESSION_PATH = "sticky-session-path";
    String STICKY_SESSION_FORCE = "sticky-session-force";
    String STICKY_SESSION_REMOVE= "sticky-session-remove";
    String WAIT_WORKER = "wait-worker";
    String MAX_ATTEMPTS = "max-attempts";
    String FLUSH_PACKETS = "flush-packets";
    String QUEUE_NEW_REQUESTS = "queue-new-requests";
    String STOP = "stop";
    String ENABLE_NODES = "enable-nodes";
    String DISABLE_NODES = "disable-nodes";
    String STOP_NODES = "stop-nodes";
    String DEFAULT_SESSION_TIMEOUT = "default-session-timeout";
    String PREDICATE = "predicate";
    String SSL_SESSION_CACHE_SIZE = "ssl-session-cache-size";
    String SSL_SESSION_TIMEOUT = "ssl-session-timeout";
    String VERIFY_CLIENT = "verify-client";
    String ENABLED_CIPHER_SUITES = "enabled-cipher-suites";
    String ENABLED_PROTOCOLS = "enabled-protocols";
    String ENABLE_HTTP2 = "enable-http2";
    String ENABLE_SPDY = "enable-spdy";
    String URI = "uri";
    String ALIASES = "aliases";
    String ELECTED = "elected";
    String PROACTIVE_AUTHENTICATION = "proactive-authentication";
    String SESSION_ID_LENGTH = "session-id-length";
    String EXTENDED = "extended";
    String MAX_BUFFERED_REQUEST_SIZE = "max-buffered-request-size";
    String MAX_SESSIONS = "max-sessions";
    String USER_AGENTS = "user-agents";
    String SESSION_TIMEOUT = "session-timeout";
    String CRAWLER_SESSION_MANAGEMENT = "crawler-session-management";
    String MAX_AJP_PACKET_SIZE = "max-ajp-packet-size";
    String STATISTICS_ENABLED = "statistics-enabled";
    String DEFAULT_SECURITY_DOMAIN = "default-security-domain";
    String DISABLE_FILE_WATCH_SERVICE = "disable-file-watch-service";
    String DISABLE_SESSION_ID_REUSE = "disable-session-id-reuse";
    String PER_MESSAGE_DEFLATE = "per-message-deflate";
    String DEFLATER_LEVEL = "deflater-level";
    String MAX_RETRIES = "max-retries";

    // Elytron Integration
    String APPLICATION_SECURITY_DOMAIN = "application-security-domain";
    String APPLICATION_SECURITY_DOMAINS = "application-security-domains";
    String HTTP_AUTHENITCATION_FACTORY = "http-authentication-factory";
    String OVERRIDE_DEPLOYMENT_CONFIG = "override-deployment-config";
    String REFERENCING_DEPLOYMENTS = "referencing-deployments";
    String SECURITY_DOMAIN = "security-domain";
    String ENABLE_JACC = "enable-jacc";

    String FILE_CACHE_MAX_FILE_SIZE = "file-cache-max-file-size";
    String FILE_CACHE_METADATA_SIZE = "file-cache-metadata-size";
    String FILE_CACHE_TIME_TO_LIVE =  "file-cache-time-to-live";
    String SESSION_ID = "session-id";
    String ATTRIBUTE = "attribute";
    String INVALIDATE_SESSION = "invalidate-session";
    String LIST_SESSIONS = "list-sessions";
    String LIST_SESSION_ATTRIBUTE_NAMES = "list-session-attribute-names";
    String LIST_SESSION_ATTRIBUTES = "list-session-attributes";
    String GET_SESSION_ATTRIBUTE = "get-session-attribute";
    String GET_SESSION_LAST_ACCESSED_TIME = "get-session-last-accessed-time";
    String GET_SESSION_LAST_ACCESSED_TIME_MILLIS = "get-session-last-accessed-time-millis";
    String GET_SESSION_CREATION_TIME = "get-session-creation-time";
    String GET_SESSION_CREATION_TIME_MILLIS = "get-session-creation-time-millis";
    String DEFAULT_COOKIE_VERSION = "default-cookie-version";

    String PROXY_PROTOCOL = "proxy-protocol";
    String MAX_POOL_SIZE = "max-pool-size";
    String THREAD_LOCAL_CACHE_SIZE = "thread-local-cache-size";
    String DIRECT = "direct";
    String LEAK_DETECTION_PERCENT = "leak-detection-percent";
    String BYTE_BUFFER_POOL = "byte-buffer-pool";
}
