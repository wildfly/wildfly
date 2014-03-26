/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
    String PATH = "path";
    String HTTP_LISTENER = "http-listener";
    String HTTPS_LISTENER = "https-listener";
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
}
