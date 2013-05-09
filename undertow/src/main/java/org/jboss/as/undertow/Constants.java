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

package org.jboss.as.undertow;

/**
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a> (c) 2012 Red Hat Inc.
 */

public interface Constants {
    String AJP_LISTENER = "ajp-listener";
    String BUFFER_CACHE = "buffer-cache";
    String BUFFER_CACHES = "buffer-caches";
    String BUFFER_SIZE = "buffer-size";
    String BUFFERS_PER_REGION = "buffers-per-region";
    String MAX_REGIONS = "max-regions";
    String BUFFER_POOL = "buffer-pool";
    String SETTING = "setting";
    String SECURITY_REALM = "security-realm";
    String SOCKET_BINDING = "socket-binding";
    String PATH = "path";
    String HTTP_LISTENER = "http-listener";
    String HTTPS_LISTENER = "https-listener";
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
    String CHAIN = "chain";
    String CHAINS = "chains";
    String CHAIN_REF = "chain-ref";
    String PROPERTIES = "properties";
    String CLASS = "class";
    String PROPERTY = "property";
    String PATHS = "paths";
    String DEFAULT_HOST = "default-host";
    String DEFAULT_VIRTUAL_HOST = "default-virtual-host";
    String DEFAULT_SERVLET_CONTAINER = "default-servlet-container";
    String DEFAULT_SERVER = "default-server";
    String ALIAS = "alias";
    /*JSP config */
    String CHECK_INTERVAL = "check-interval";
    String CONTAINER = "container";
    String DEVELOPMENT = "development";
    String DISABLED = "disabled";
    String DISPLAY_SOURCE_FRAGMENT = "display-source-fragment";
    String DUMP_SMAP = "dump-smap";
    String ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE = "error-on-use-bean-invalid-class-attribute";
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
}
