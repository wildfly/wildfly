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
package org.jboss.as.connector.subsystems.datasources;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class Constants {

    static final String DATASOURCES_SUBSYTEM = "datasources-subsystem";

    static final String DATASOURCES = "datasources";

    static final String DATASOURCE = "datasource";

    static final String XA_DATASOURCES = "xa-datasources";

    static final String XA_DATASOURCE = "xa-datasource";

    static final String CONNECTION_URL = "connection-url";

    static final String JDBC_DRIVER = "jdbc-driver";

    static final String DRIVER_CLASS = "driver-class";

    static final String DRIVER = "driver";

    static final String CONNECTION_PROPERTIES = "connection-properties";

    static final String CONNECTION_PROPERTY = "connection-property";

    static final String PROPERTY_NAME = "property-name";

    static final String PROPERTY_VALUE = "property-value";

    static final String NEW_CONNECTION_SQL = "new-connection-sql";

    static final String TRANSACTION_ISOLOATION = "transaction-isolation";

    static final String URL_DELIMITER = "url-delimiter";

    static final String URL_SELECTOR_STRATEGY_CLASS_NAME = "url-selector-strategy-class-name";

    static final String USE_JAVA_CONTEXT = "use-java-context";

    static final String POOLNAME = "pool-name";

    static final String ENABLED = "enabled";

    static final String JNDINAME = "jndi-name";

    static final String URLDELIMITER = "url-delimiter";

    static final String MIN_POOL_SIZE = "min-pool-size";

    static final String MAX_POOL_SIZE = "max-pool-size";

    static final String POOL_PREFILL = "pool-prefill";

    static final String POOL_USE_STRICT_MIN = "pool-use-strict-min";

    static final String ALLOCATION_RETRY = "allocation-retry";

    static final String ALLOCATION_RETRY_WAIT_MILLIS = "allocation-retry-wait-millis";

    static final String BLOCKING_TIMEOUT_WAIT_MILLIS = "blocking-timeout-wait-millis";

    static final String IDLETIMEOUTMINUTES = "idle-timeout-minutes";

    static final String SETTXQUERYTIMEOUT = "set-tx-query-timeout";

    static final String XA_RESOURCE_TIMEOUT = "xa-resource-timeout";

    static final String QUERYTIMEOUT = "query-timeout";

    static final String USETRYLOCK = "use-try-lock";

    static final String USERNAME = "user-name";

    static final String PASSWORD = "password";

    static final String SECURITY_DOMAIN = "security-domain";

    static final String SHAREPREPAREDSTATEMENTS = "share-prepared-statements";

    static final String PREPAREDSTATEMENTSCACHESIZE = "prepared-statements-cacheSize";

    static final String TRACKSTATEMENTS = "track-statements";

    static final String VALIDCONNECTIONCHECKERCLASSNAME = "valid-connection-checker-class-name";

    static final String CHECKVALIDCONNECTIONSQL = "check-valid-connection-sql";

    static final String VALIDATEONMATCH = "validate-on-match";

    static final String SPY = "spy";

    static final String STALECONNECTIONCHECKERCLASSNAME = "stale-connection-checker-class-name";

    static final String BACKGROUNDVALIDATIONMINUTES = "background-validation-minutes";

    static final String BACKGROUNDVALIDATION = "background-validation";

    static final String USE_FAST_FAIL = "use-fast-fail";

    static final String EXCEPTIONSORTERCLASSNAME = "exception-sorter-class-name";

    static final String XADATASOURCEPROPERTIES = "xa-data-source-properties";

    static final String XADATASOURCEPROPERTY = "xa-data-source-property";

    static final String XADATASOURCECLASS = "xa-data-source-class";

    static final String INTERLIVING = "interliving";

    static final String NOTXSEPARATEPOOL = "no-tx-separate-pool";

    static final String PAD_XID = "pad-xid";

    static final String SAME_RM_OVERRIDE = "same-rm-override";

    static final String WRAP_XA_DATASOURCE = "wrap-xa-datasource";

    static final String NEWCONNECTIONSQL = "new-connection-sql";

    static final String EXCEPTIONSORTER_PROPERTIES = "exceptionsorter-properties";

    static final String STALECONNECTIONCHECKER_PROPERTIES = "staleconnectionchecker-properties";

    static final String VALIDCONNECTIONCHECKER_PROPERTIES = "validconnectionchecker-properties";

    static final String DATA_SOURCE = "data-source";

    static final String XA_DATA_SOURCE = "xa-data-source";

    static final String REAUTHPLUGIN_CLASSNAME = "reauth-plugin-class-name";

    static final String REAUTHPLUGIN_PROPERTIES = "reauth-plugin-properties";

    static final String RECOVERY_USERNAME = "recovery-username";

    static final String RECOVERY_PASSWORD = "recovery-password";

    static final String RECOVERY_SECURITY_DOMAIN = "recovery-security-domain";

    static final String RECOVERLUGIN_CLASSNAME = "recovery-plugin-properties";

    static final String RECOVERLUGIN_PROPERTIES = "recovery-plugin-properties";

    static final String NO_RECOVERY = "no-recovery";

}
