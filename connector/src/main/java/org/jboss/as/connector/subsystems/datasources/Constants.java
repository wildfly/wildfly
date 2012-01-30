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

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.DataSource;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.DsSecurity;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.XaDataSource;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class Constants {

    static final String DATASOURCES = "datasources";

    static final String DATA_SOURCE = "data-source";

    static final String XA_DATASOURCE = "xa-data-source";

    private static final String CONNECTION_URL_NAME = "connection-url";

    static final String JDBC_DRIVER_NAME = "jdbc-driver";

    private static final String DATASOURCE_DRIVER_CLASS_NAME = "driver-class";

    private static final String DATASOURCE_CLASS_NAME = "datasource-class";

    private static final String DATASOURCE_DRIVER_NAME = "driver-name";

    private static final String DRIVER_NAME_NAME = "driver-name";

    private static final String DRIVER_MODULE_NAME_NAME = "driver-module-name";

    private static final String DRIVER_MAJOR_VERSION_NAME = "driver-major-version";

    private static final String DRIVER_MINOR_VERSION_NAME = "driver-minor-version";

    private static final String DRIVER_CLASS_NAME_NAME = "driver-class-name";

    private static final String DRIVER_DATASOURCE_CLASS_NAME_NAME = "driver-datasource-class-name";

    private static final String DRIVER_XA_DATASOURCE_CLASS_NAME_NAME = "driver-xa-datasource-class-name";

    private static final String CONNECTION_PROPERTIES_NAME = "connection-properties";

    private static final String CONNECTION_PROPERTY_VALUE_NAME = "value";

    private static final String NEW_CONNECTION_SQL_NAME = "new-connection-sql";

    private static final String TRANSACTION_ISOLATION_NAME = "transaction-isolation";

    private static final String URL_DELIMITER_NAME = "url-delimiter";

    private static final String URL_SELECTOR_STRATEGY_CLASS_NAME_NAME = "url-selector-strategy-class-name";

    private static final String USE_JAVA_CONTEXT_NAME = "use-java-context";

    static final String POOLNAME_NAME = "pool-name";

    private static final String ENABLED_NAME = "enabled";

    private static final String JTA_NAME = "jta";

    private static final String JNDINAME_NAME = "jndi-name";

    private static final String ALLOCATION_RETRY_NAME = "allocation-retry";

    private static final String ALLOCATION_RETRY_WAIT_MILLIS_NAME = "allocation-retry-wait-millis";

    private static final String SETTXQUERYTIMEOUT_NAME = "set-tx-query-timeout";

    private static final String XA_RESOURCE_TIMEOUT_NAME = "xa-resource-timeout";

    private static final String QUERYTIMEOUT_NAME = "query-timeout";

    private static final String USETRYLOCK_NAME = "use-try-lock";

    private static final String USERNAME_NAME = "user-name";

    private static final String PASSWORD_NAME = "password";

    private static final String SECURITY_DOMAIN_NAME = "security-domain";

    private static final String SHAREPREPAREDSTATEMENTS_NAME = "share-prepared-statements";

    private static final String PREPAREDSTATEMENTSCACHESIZE_NAME = "prepared-statements-cache-size";

    private static final String TRACKSTATEMENTS_NAME = "track-statements";

    private static final String VALIDCONNECTIONCHECKERCLASSNAME_NAME = "valid-connection-checker-class-name";

    private static final String CHECKVALIDCONNECTIONSQL_NAME = "check-valid-connection-sql";

    private static final String VALIDATEONMATCH_NAME = "validate-on-match";

    private static final String SPY_NAME = "spy";

    private static final String USE_CCM_NAME = "use-ccm";

    private static final String STALECONNECTIONCHECKERCLASSNAME_NAME = "stale-connection-checker-class-name";

    private static final String EXCEPTIONSORTERCLASSNAME_NAME = "exception-sorter-class-name";

    private static final String XADATASOURCEPROPERTIES_NAME = "xa-datasource-properties";

    private static final String XADATASOURCEPROPERTIES_VALUE_NAME = "value";

    private static final String XADATASOURCECLASS_NAME = "xa-datasource-class";

    private static final String INTERLEAVING_NAME = "interleaving";

    private static final String NOTXSEPARATEPOOL_NAME = "no-tx-separate-pool";

    private static final String PAD_XID_NAME = "pad-xid";

    private static final String SAME_RM_OVERRIDE_NAME = "same-rm-override";

    private static final String WRAP_XA_RESOURCE_NAME = "wrap-xa-resource";

    private static final String EXCEPTIONSORTER_PROPERTIES_NAME = "exception-sorter-properties";

    private static final String STALECONNECTIONCHECKER_PROPERTIES_NAME = "stale-connection-checker-properties";

    private static final String VALIDCONNECTIONCHECKER_PROPERTIES_NAME = "valid-connection-checker-properties";

    private static final String REAUTHPLUGIN_CLASSNAME_NAME = "reauth-plugin-class-name";

    private static final String REAUTHPLUGIN_PROPERTIES_NAME = "reauth-plugin-properties";

    private static final String RECOVERY_USERNAME_NAME = "recovery-username";

    private static final String RECOVERY_PASSWORD_NAME = "recovery-password";

    private static final String RECOVERY_SECURITY_DOMAIN_NAME = "recovery-security-domain";

    private static final String RECOVER_PLUGIN_CLASSNAME_NAME = "recovery-plugin-class-name";

    private static final String RECOVER_PLUGIN_PROPERTIES_NAME = "recovery-plugin-properties";

    private static final String NO_RECOVERY_NAME = "no-recovery";

    static final String INSTALLED_DRIVERS = "installed-drivers";

    static final String DEPLOYMENT_NAME = "deployment-name";

    static final String MODULE_SLOT = "module-slot";

    static final String JDBC_COMPLIANT = "jdbc-compliant";

    static final String STATISTICS = "statistics";


    static SimpleAttributeDefinition CONNECTION_URL = new SimpleAttributeDefinition(CONNECTION_URL_NAME, DataSource.Tag.CONNECTION_URL.getLocalName(),  new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition DRIVER_CLASS = new SimpleAttributeDefinition(DATASOURCE_DRIVER_CLASS_NAME, DataSource.Tag.DRIVER_CLASS.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition DATASOURCE_CLASS = new SimpleAttributeDefinition(DATASOURCE_CLASS_NAME, DataSource.Tag.DATASOURCE_CLASS.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition JNDINAME = new SimpleAttributeDefinition(JNDINAME_NAME, DataSource.Attribute.JNDI_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE, new ParameterValidator() {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if (value.isDefined() && value.getType() != ModelType.EXPRESSION) {
            String str = value.asString();
            if (! str.startsWith("java:/") && ! str.startsWith("java:jboss/")) {
                throw new OperationFailedException(new ModelNode().set("Jndi name have to start with java:/ or java:jboss/"));
            }
        }
        }

        @Override
        public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
            //TODO implement validateResolvedParameter
            throw new UnsupportedOperationException();
        }
    });

    static SimpleAttributeDefinition DATASOURCE_DRIVER = new SimpleAttributeDefinition(DATASOURCE_DRIVER_NAME, DataSource.Tag.DRIVER.getLocalName(),  new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NEW_CONNECTION_SQL = new SimpleAttributeDefinition(NEW_CONNECTION_SQL_NAME, DataSource.Tag.NEW_CONNECTION_SQL.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition URL_DELIMITER = new SimpleAttributeDefinition(URL_DELIMITER_NAME, DataSource.Tag.URL_DELIMITER.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition URL_SELECTOR_STRATEGY_CLASS_NAME = new SimpleAttributeDefinition(URL_SELECTOR_STRATEGY_CLASS_NAME_NAME, DataSource.Tag.URL_SELECTOR_STRATEGY_CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_JAVA_CONTEXT = new SimpleAttributeDefinition(USE_JAVA_CONTEXT_NAME, DataSource.Attribute.USE_JAVA_CONTEXT.getLocalName(), new ModelNode().set(Defaults.USE_JAVA_CONTEXT), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinition(ENABLED_NAME, DataSource.Attribute.ENABLED.getLocalName(), new ModelNode().set(Defaults.ENABLED), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition JTA = new SimpleAttributeDefinition(JTA_NAME, DataSource.Attribute.JTA.getLocalName(), new ModelNode().set(Defaults.JTA), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition CONNECTION_PROPERTIES = new SimpleAttributeDefinition(CONNECTION_PROPERTIES_NAME, DataSource.Tag.CONNECTION_PROPERTY.getLocalName(),  new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition CONNECTION_PROPERTY_VALUE = new SimpleAttributeDefinition(CONNECTION_PROPERTY_VALUE_NAME, DataSource.Tag.CONNECTION_PROPERTY.getLocalName(),  new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USERNAME = new SimpleAttributeDefinition(USERNAME_NAME, Credential.Tag.USER_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinition(PASSWORD_NAME, Credential.Tag.PASSWORD.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinition(SECURITY_DOMAIN_NAME, CommonSecurity.Tag.SECURITY_DOMAIN.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PREPAREDSTATEMENTSCACHESIZE = new SimpleAttributeDefinition(PREPAREDSTATEMENTSCACHESIZE_NAME, Statement.Tag.PREPARED_STATEMENT_CACHE_SIZE.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SHAREPREPAREDSTATEMENTS = new SimpleAttributeDefinition(SHAREPREPAREDSTATEMENTS_NAME, Statement.Tag.SHARE_PREPARED_STATEMENTS.getLocalName(), new ModelNode().set(Defaults.SHARE_PREPARED_STATEMENTS), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition TRACKSTATEMENTS = new SimpleAttributeDefinition(TRACKSTATEMENTS_NAME, Statement.Tag.TRACK_STATEMENTS.getLocalName(), new ModelNode().set(Defaults.TRACK_STATEMENTS.name()), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY = new SimpleAttributeDefinition(ALLOCATION_RETRY_NAME, TimeOut.Tag.ALLOCATION_RETRY.getLocalName(),  new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY_WAIT_MILLIS = new SimpleAttributeDefinition(ALLOCATION_RETRY_WAIT_MILLIS_NAME, TimeOut.Tag.ALLOCATION_RETRY_WAIT_MILLIS.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);


    static SimpleAttributeDefinition QUERYTIMEOUT = new SimpleAttributeDefinition(QUERYTIMEOUT_NAME, TimeOut.Tag.QUERY_TIMEOUT.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USETRYLOCK = new SimpleAttributeDefinition(USETRYLOCK_NAME, TimeOut.Tag.USE_TRY_LOCK.getLocalName(),  new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SETTXQUERYTIMEOUT = new SimpleAttributeDefinition(SETTXQUERYTIMEOUT_NAME, TimeOut.Tag.SET_TX_QUERY_TIMEOUT.getLocalName(), new ModelNode().set(Defaults.SET_TX_QUERY_TIMEOUT), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition TRANSACTION_ISOLATION = new SimpleAttributeDefinition(TRANSACTION_ISOLATION_NAME, DataSource.Tag.TRANSACTION_ISOLATION.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition CHECKVALIDCONNECTIONSQL = new SimpleAttributeDefinition(CHECKVALIDCONNECTIONSQL_NAME, Validation.Tag.CHECK_VALID_CONNECTION_SQL.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition EXCEPTIONSORTERCLASSNAME = new SimpleAttributeDefinition(EXCEPTIONSORTERCLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition EXCEPTIONSORTER_PROPERTIES = new SimpleAttributeDefinition(EXCEPTIONSORTER_PROPERTIES_NAME, org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.OBJECT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition STALECONNECTIONCHECKERCLASSNAME = new SimpleAttributeDefinition(STALECONNECTIONCHECKERCLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition STALECONNECTIONCHECKER_PROPERTIES = new SimpleAttributeDefinition(STALECONNECTIONCHECKER_PROPERTIES_NAME, org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.OBJECT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition VALIDCONNECTIONCHECKERCLASSNAME = new SimpleAttributeDefinition(VALIDCONNECTIONCHECKERCLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition VALIDCONNECTIONCHECKER_PROPERTIES = new SimpleAttributeDefinition(VALIDCONNECTIONCHECKER_PROPERTIES_NAME, org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.OBJECT, true, true, MeasurementUnit.NONE);


    static SimpleAttributeDefinition VALIDATEONMATCH = new SimpleAttributeDefinition(VALIDATEONMATCH_NAME, Validation.Tag.VALIDATE_ON_MATCH.getLocalName(),new ModelNode().set(Defaults.VALIDATE_ON_MATCH), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SPY = new SimpleAttributeDefinition(SPY_NAME, DataSource.Attribute.SPY.getLocalName(), new ModelNode().set(Defaults.SPY), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_CCM = new SimpleAttributeDefinition(USE_CCM_NAME, DataSource.Attribute.USE_CCM.getLocalName(), new ModelNode().set(Defaults.USE_CCM), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XADATASOURCECLASS = new SimpleAttributeDefinition(XADATASOURCECLASS_NAME, XaDataSource.Tag.XA_DATASOURCE_CLASS.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition INTERLEAVING = new SimpleAttributeDefinition(INTERLEAVING_NAME, CommonXaPool.Tag.INTERLEAVING.getLocalName(), new ModelNode().set(Defaults.INTERLEAVING), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NOTXSEPARATEPOOL = new SimpleAttributeDefinition(NOTXSEPARATEPOOL_NAME, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS.getLocalName(), new ModelNode().set(Defaults.NO_TX_SEPARATE_POOL), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PAD_XID = new SimpleAttributeDefinition(PAD_XID_NAME, CommonXaPool.Tag.PAD_XID.getLocalName(), new ModelNode().set(Defaults.PAD_XID), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SAME_RM_OVERRIDE = new SimpleAttributeDefinition(SAME_RM_OVERRIDE_NAME, CommonXaPool.Tag.IS_SAME_RM_OVERRIDE.getLocalName(), new ModelNode(), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition WRAP_XA_RESOURCE = new SimpleAttributeDefinition(WRAP_XA_RESOURCE_NAME, CommonXaPool.Tag.WRAP_XA_RESOURCE.getLocalName(), new ModelNode().set(Defaults.WRAP_XA_RESOURCE), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XA_RESOURCE_TIMEOUT = new SimpleAttributeDefinition(XA_RESOURCE_TIMEOUT_NAME, TimeOut.Tag.XA_RESOURCE_TIMEOUT.getLocalName(),  new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition REAUTHPLUGIN_CLASSNAME = new SimpleAttributeDefinition(REAUTHPLUGIN_CLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition REAUTHPLUGIN_PROPERTIES = new SimpleAttributeDefinition(REAUTHPLUGIN_PROPERTIES_NAME, org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.OBJECT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_USERNAME = new SimpleAttributeDefinition(RECOVERY_USERNAME_NAME, Credential.Tag.USER_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_PASSWORD = new SimpleAttributeDefinition(RECOVERY_PASSWORD_NAME, Credential.Tag.PASSWORD.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_SECURITY_DOMAIN = new SimpleAttributeDefinition(RECOVERY_SECURITY_DOMAIN_NAME, Credential.Tag.SECURITY_DOMAIN.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERLUGIN_CLASSNAME = new SimpleAttributeDefinition(RECOVER_PLUGIN_CLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERLUGIN_PROPERTIES = new SimpleAttributeDefinition(RECOVER_PLUGIN_PROPERTIES_NAME, org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName(),  new ModelNode(), ModelType.OBJECT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NO_RECOVERY = new SimpleAttributeDefinition(NO_RECOVERY_NAME, Recovery.Attribute.NO_RECOVERY.getLocalName(),  new ModelNode(), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XADATASOURCE_PROPERTIES = new SimpleAttributeDefinition(XADATASOURCEPROPERTIES_NAME, XaDataSource.Tag.XA_DATASOURCE_PROPERTY.getLocalName(),  new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XADATASOURCE_PROPERTY_VALUE = new SimpleAttributeDefinition(XADATASOURCEPROPERTIES_VALUE_NAME, XaDataSource.Tag.XA_DATASOURCE_PROPERTY.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_NAME = new SimpleAttributeDefinition(DRIVER_NAME_NAME, Driver.Attribute.NAME.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_MODULE_NAME = new SimpleAttributeDefinition(DRIVER_MODULE_NAME_NAME, Driver.Attribute.MODULE.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_MAJOR_VERSION = new SimpleAttributeDefinition(DRIVER_MAJOR_VERSION_NAME, Driver.Attribute.MAJOR_VERSION.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_MINOR_VERSION = new SimpleAttributeDefinition(DRIVER_MINOR_VERSION_NAME, Driver.Attribute.MINOR_VERSION.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_CLASS_NAME = new SimpleAttributeDefinition(DRIVER_CLASS_NAME_NAME, Driver.Tag.DRIVER_CLASS.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_DATASOURCE_CLASS_NAME = new SimpleAttributeDefinition(DRIVER_DATASOURCE_CLASS_NAME_NAME, Driver.Tag.DATASOURCE_CLASS.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_XA_DATASOURCE_CLASS_NAME = new SimpleAttributeDefinition(DRIVER_XA_DATASOURCE_CLASS_NAME_NAME, Driver.Tag.XA_DATASOURCE_CLASS.getLocalName(),  new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

}
