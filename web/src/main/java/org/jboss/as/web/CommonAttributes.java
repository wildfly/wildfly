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

package org.jboss.as.web;

/**
 * @author Emanuel Muckenhuber
 */
interface CommonAttributes {

    String ACCESS_LOG = "access-log";
    String ALIAS = "alias";
    String CHECK_INTERVAL = "check-interval";
    String CONNECTOR = "connector";
    String CONTAINER_CONFIG = "config";
    String DEFAULT_HOST = "default-host";
    String DEVELOPMENT = "development";
    String DIRECTORY = "directory";
    String DISABLED = "disabled";
    String DISPLAY_SOURCE_FRAGMENT = "display-source-fragment";
    String DUMP_SMAP = "dump-smap";
    String ENABLED = "enabled";
    String ENABLE_LOOKUPS = "enable-lookups";
    String ERROR_ON_USE_BEAN_INVALID_CLASS_ATTRIBUTE = "error-on-use-bean-invalid-class-attribute";
    String EXECUTOR = "executor";
    String EXTENDED = "extended";
    String FILE_ENCONDING = "file-encoding";
    String GENERATE_STRINGS_AS_CHAR_ARRAYS = "generate-strings-as-char-arrays";
    String JAVA_ENCODING = "java-encoding";
    String JSP_CONFIGURATION = "jsp-configuration";
    String KEEP_GENERATED = "keep-generated";
    String LISTINGS = "listings";
    String MAPPED_FILE = "mapped-file";
    String MAX_DEPTH = "max-depth";
    String MAX_POST_SIZE = "max-post-size";
    String MAX_SAVE_POST_SIZE = "max-save-post-size";
    String MIME_MAPPING = "mime-mapping";
    String MODIFIFICATION_TEST_INTERVAL = "modification-test-interval";
    String NAME = "name";
    String PATH = "path";
    String PATTERN = "pattern";
    String PREFIX = "prefix";
    String PROTOCOL = "protocol";
    String PROXY_NAME = "proxy-name";
    String PROXY_PORT = "proxy-port";
    String READ_ONLY = "read-only";
    String RECOMPILE_ON_FAIL = "recompile-on-fail";
    String REDIRECT_PORT = "redirect-por";
    String RELATIVE_TO = "relative-to";
    String RESOLVE_HOSTS = "resolve-hosts";
    String REWRITE = "rewrite";
    String ROTATE = "rotate";
    String SCHEME = "scheme";
    String SCRATCH_DIR = "scratch-dir";
    String SECRET = "secret";
    String SECURE = "secure";
    String SENDFILE = "sendfile";
    String SMAP = "smap";
    String SOCKET_BINDING = "socket-binding";
    String SOURCE_VM = "source-vm";
    String STATIC_RESOURCES = "static-resources";
    String SUBSYSTEM = "subsystem";
    String TAG_POOLING = "tag-pooling";
    String TARGET_VM = "target-vm";
    String TRIM_SPACES = "trim-spaces";
    String VALUE = "value";
    String VIRTUAL_SERVER = "virtual-server";
    String WEBDAV = "webdav";
    String WELCOME_FILE = "welcome-file";
    String X_POWERED_BY = "x-powered-by";

}
