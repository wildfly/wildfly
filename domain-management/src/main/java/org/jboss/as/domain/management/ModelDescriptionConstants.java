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

package org.jboss.as.domain.management;

/**
 * Constants specific to the Domain Management module.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
public class ModelDescriptionConstants {

    public static final String ALIAS = "alias";
    public static final String ALLOWED_USERS = "allowed-users";
    public static final String AUTHENTICATION = "authentication";
    public static final String AUTHORIZATION = "authorization";
    public static final String BY_ACCESS_TIME = "by-access-time";
    public static final String BY_SEARCH_TIME = "by-search-time";
    public static final String CACHE = "cache";
    public static final String CACHE_FAILURES = "cache-failures";
    public static final String CACHE_SIZE = "cache-size";
    public static final String CONTAINS = "contains";
    public static final String DEFAULT_USER = "default-user";
    public static final String DEFAULT_DEFAULT_USER = "$local";
    public static final String DISTINGUISHED_NAME = "distinguished-name";
    public static final String EVICTION_TIME = "eviction-time";
    public static final String FLUSH_CACHE = "flush-cache";
    public static final String GROUPS = "groups";
    public static final String IDENTITY = "identity";
    public static final String IS_CALLER_IN_ROLE = "is-caller-in-role";
    public static final String JKS = "JKS";
    public static final String KEY_PASSWORD = "key-password";
    public static final String KEYSTORE_PASSWORD = "keystore-password";
    public static final String KEYSTORE_PATH = "keystore-path";
    public static final String KEYSTORE_PROVIDER = "keystore-provider";
    public static final String KEYSTORE_RELATIVE_TO = "keystore-relative-to";
    public static final String LDAP = "ldap";
    public static final String LOCAL = "local";
    public static final String MAPPED_ROLES = "mapped-roles";
    public static final String MAX_CACHE_SIZE = "max-cache-size";
    public static final String MECHANISM = "mechanism";
    public static final String NAME = "name";
    public static final String PASSWORD = "password";
    public static final String PATH = "path";
    public static final String PLUG_IN = "plug-in";
    public static final String PROPERTY = "property";
    public static final String PROVIDER = "provider";
    public static final String REALM = "realm";
    public static final String RELATIVE_TO = "relative-to";
    public static final String ROLES = "roles";
    public static final String USERNAME = "username";
    public static final String VALUE = "value";
    public static final String VERBOSE = "verbose";
    public static final String WHOAMI = "whoami";

    private ModelDescriptionConstants() {}

}
