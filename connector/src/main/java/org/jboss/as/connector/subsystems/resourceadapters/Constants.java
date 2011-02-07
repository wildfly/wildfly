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
package org.jboss.as.connector.subsystems.resourceadapters;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class Constants {

    static final String RESOURCEADAPTER = "resource-adapter";

    static final String RESOURCEADAPTERS = "resource-adapters";

    static final String CONNECTIONDEFINITIONS = "connection-definitions";

    static final String CLASS_NAME = "class-name";

    static final String JNDI_NAME = "jndi-name";

    static final String POOL_NAME = "pool-name";

    static final String NEW_CONNECTION_SQL = "new-connection-sql";

    static final String TRANSACTION_ISOLOATION = "transaction-isolation";

    static final String URL_DELIMITER = "url-delimiter";

    static final String URL_SELECTOR_STRATEGY_CLASS_NAME = "url-selector-strategy-class-name";

    static final String USE_JAVA_CONTEXT = "use-java-context";

    static final String POOLNAME = "pool-name";

    static final String ENABLED = "enabled";

    static final String JNDINAME = "jndi-name";

    static final String URLDELIMITER = "url-delimiter";

    static final String POOL = "pool";

    static final String MIN_POOL_SIZE = "min-pool-size";

    static final String MAX_POOL_SIZE = "max-pool-size";

    static final String POOL_PREFILL = "pool-prefill";

    static final String POOL_USE_STRICT_MIN = "pool-use-strict-min";

    static final String TIMEOUT = "time-out";

    static final String ALLOCATION_RETRY = "allocation-retry";

    static final String ALLOCATION_RETRY_WAIT_MILLIS = "allocation-retry-wait-millis";

    static final String BLOCKING_TIMEOUT_WAIT_MILLIS = "blocking-timeout-wait-millis";

    static final String IDLETIMEOUTMINUTES = "idle-timeout-minutes";

    static final String SETTXQUERTTIMEOUT = "set-tx-quert-timeout";

    static final String XA_RESOURCE_TIMEOUT = "xa-resource-timeout";

    static final String QUERYTIMEOUT = "query-timeout";

    static final String USETRYLOCK = "use-try-lock";

    static final String SECURITY = "security";

    static final String USERNAME = "user-name";

    static final String PASSWORD = "password";

    static final String STATEMENT = "statement";

    static final String SHAREPREPAREDSTATEMENTS = "share-prepared-statements";

    static final String PREPAREDSTATEMENTSCACHESIZE = "prepared-statements-cacheSize";

    static final String TRACKSTATEMENTS = "track-statements";

    static final String VALIDATION = "validation";

    static final String VALIDCONNECTIONCHECKERCLASSNAME = "valid-connection-checker-class-name";

    static final String CHECKVALIDCONNECTIONSQL = "check-valid-connection-sql";

    static final String VALIDATEONMATCH = "validate-on-match";

    static final String STALECONNECTIONCHECKERCLASSNAME = "stale-connection-checker-class-name";

    static final String BACKGROUNDVALIDATIONMINUTES = "background-validation-minutes";

    static final String BACKGROUNDVALIDATION = "background-validation";

    static final String USE_FAST_FAIL = "use-fast-fail";

    static final String CONFIG_PROPERTIES = "config-properties";

    static final String ARCHIVE = "archive";

    static final String BOOTSTRAPCONTEXT = "bootstrapcontext";

    static final String TRANSACTIONSUPPORT = "transaction-support";

    static final String BEANVALIDATIONGROUPS = "beanvalidationgroups";

    static final String EXCEPTIONSORTERCLASSNAME = "exception-sorter-class-name";

    static final String ADMIN_OBJECTS = "admin-objects";
}
