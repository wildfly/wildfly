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

import static org.jboss.as.connector.logging.ConnectorMessages.MESSAGES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;

import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PropertiesAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinition;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.NonResolvingResourceDescriptionResolver;
import org.jboss.as.controller.operations.validation.ParameterValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.common.api.metadata.Defaults;
import org.jboss.jca.common.api.metadata.common.CommonSecurity;
import org.jboss.jca.common.api.metadata.common.CommonXaPool;
import org.jboss.jca.common.api.metadata.common.Credential;
import org.jboss.jca.common.api.metadata.common.Recovery;
import org.jboss.jca.common.api.metadata.ds.Driver;
import org.jboss.jca.common.api.metadata.ds.Statement;
import org.jboss.jca.common.api.metadata.ds.TimeOut;
import org.jboss.jca.common.api.metadata.ds.Validation;
import org.jboss.jca.common.api.metadata.ds.v11.DataSource;
import org.jboss.jca.common.api.metadata.ds.v11.DsPool;
import org.jboss.jca.common.api.metadata.ds.v11.XaDataSource;

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

    private static final String ALLOW_MULTIPLE_USERS_NAME = "allow-multiple-users";

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

    private static final String VALID_CONNECTION_CHECKER_CLASSNAME_NAME = "valid-connection-checker-class-name";

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

    static final SimpleAttributeDefinition DEPLOYMENT_NAME = SimpleAttributeDefinitionBuilder.create("deployment-name", ModelType.STRING)
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();

    static final SimpleAttributeDefinition MODULE_SLOT = SimpleAttributeDefinitionBuilder.create("module-slot", ModelType.STRING)
            .setAllowExpression(false)
            .setAllowNull(true)
            .build();

    static final SimpleAttributeDefinition JDBC_COMPLIANT = SimpleAttributeDefinitionBuilder.create("jdbc-compliant", ModelType.BOOLEAN)
            .setAllowNull(true)
            .build();

    static final String STATISTICS = "statistics";


    static SimpleAttributeDefinition CONNECTION_URL = new SimpleAttributeDefinition(CONNECTION_URL_NAME, DataSource.Tag.CONNECTION_URL.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition DRIVER_CLASS = new SimpleAttributeDefinitionBuilder(DATASOURCE_DRIVER_CLASS_NAME, ModelType.STRING)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName(DataSource.Tag.DRIVER_CLASS.getLocalName())
            .build();


    static SimpleAttributeDefinition DATASOURCE_CLASS = new SimpleAttributeDefinitionBuilder(DATASOURCE_CLASS_NAME, ModelType.STRING)
            .setXmlName(DataSource.Tag.DATASOURCE_CLASS.getLocalName())
            .setAllowExpression(true)
            .setAllowNull(true)
            .build();


    static SimpleAttributeDefinition JNDI_NAME = new SimpleAttributeDefinition(JNDINAME_NAME, DataSource.Attribute.JNDI_NAME.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE, new ParameterValidator() {
        @Override
        public void validateParameter(String parameterName, ModelNode value) throws OperationFailedException {
            if (value.isDefined()) {
                if (value.getType() != ModelType.EXPRESSION) {
                    String str = value.asString();
                    if (!str.startsWith("java:/") && !str.startsWith("java:jboss/")) {
                        throw MESSAGES.jndiNameInvalidFormat();
                    }
                }
            } else {
                throw MESSAGES.jndiNameRequired();
            }
        }

        @Override
        public void validateResolvedParameter(String parameterName, ModelNode value) throws OperationFailedException {
            validateParameter(parameterName, value.resolve());
        }
    });

    static SimpleAttributeDefinition DATASOURCE_DRIVER = new SimpleAttributeDefinition(DATASOURCE_DRIVER_NAME, DataSource.Tag.DRIVER.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NEW_CONNECTION_SQL = new SimpleAttributeDefinition(NEW_CONNECTION_SQL_NAME, DataSource.Tag.NEW_CONNECTION_SQL.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition URL_DELIMITER = new SimpleAttributeDefinition(URL_DELIMITER_NAME, DataSource.Tag.URL_DELIMITER.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition URL_SELECTOR_STRATEGY_CLASS_NAME = new SimpleAttributeDefinition(URL_SELECTOR_STRATEGY_CLASS_NAME_NAME, DataSource.Tag.URL_SELECTOR_STRATEGY_CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_JAVA_CONTEXT = new SimpleAttributeDefinition(USE_JAVA_CONTEXT_NAME, DataSource.Attribute.USE_JAVA_CONTEXT.getLocalName(), new ModelNode().set(Defaults.USE_JAVA_CONTEXT), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    //Note: ENABLED default is false in AS7 (true in IJ) because of the enable/disable operation behaviour
    static SimpleAttributeDefinition ENABLED = new SimpleAttributeDefinitionBuilder(ENABLED_NAME, ModelType.BOOLEAN)
            .setXmlName(DataSource.Attribute.ENABLED.getLocalName())
            .setAllowExpression(false)
            .setDefaultValue(new ModelNode(false))
            .setAllowNull(true)
            .build();

    static final SimpleAttributeDefinition[] READONLY_XA_DATASOURCE_ATTRIBUTE = new SimpleAttributeDefinition[]{ENABLED};
    static final SimpleAttributeDefinition[] READONLY_DATASOURCE_ATTRIBUTE = new SimpleAttributeDefinition[]{ENABLED};

    static SimpleAttributeDefinition JTA = new SimpleAttributeDefinition(JTA_NAME, DataSource.Attribute.JTA.getLocalName(), new ModelNode().set(Defaults.JTA), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition CONNECTION_PROPERTIES = new SimpleAttributeDefinition(CONNECTION_PROPERTIES_NAME, DataSource.Tag.CONNECTION_PROPERTY.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition CONNECTION_PROPERTY_VALUE = new SimpleAttributeDefinition(CONNECTION_PROPERTY_VALUE_NAME, DataSource.Tag.CONNECTION_PROPERTY.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USERNAME = new SimpleAttributeDefinition(USERNAME_NAME, Credential.Tag.USER_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PASSWORD = new SimpleAttributeDefinition(PASSWORD_NAME, Credential.Tag.PASSWORD.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SECURITY_DOMAIN = new SimpleAttributeDefinition(SECURITY_DOMAIN_NAME, CommonSecurity.Tag.SECURITY_DOMAIN.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PREPARED_STATEMENTS_CACHE_SIZE = new SimpleAttributeDefinition(PREPAREDSTATEMENTSCACHESIZE_NAME, Statement.Tag.PREPARED_STATEMENT_CACHE_SIZE.getLocalName(), new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SHARE_PREPARED_STATEMENTS = new SimpleAttributeDefinition(SHAREPREPAREDSTATEMENTS_NAME, Statement.Tag.SHARE_PREPARED_STATEMENTS.getLocalName(), new ModelNode().set(Defaults.SHARE_PREPARED_STATEMENTS), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition TRACK_STATEMENTS = new SimpleAttributeDefinition(TRACKSTATEMENTS_NAME, Statement.Tag.TRACK_STATEMENTS.getLocalName(), new ModelNode().set(Defaults.TRACK_STATEMENTS.name()), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY = new SimpleAttributeDefinition(ALLOCATION_RETRY_NAME, TimeOut.Tag.ALLOCATION_RETRY.getLocalName(), new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOCATION_RETRY_WAIT_MILLIS = new SimpleAttributeDefinition(ALLOCATION_RETRY_WAIT_MILLIS_NAME, TimeOut.Tag.ALLOCATION_RETRY_WAIT_MILLIS.getLocalName(), new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition ALLOW_MULTIPLE_USERS = new SimpleAttributeDefinition(ALLOW_MULTIPLE_USERS_NAME, DsPool.Tag.ALLOW_MULTIPLE_USERS.getLocalName(), new ModelNode(Defaults.ALLOW_MULTIPLE_USERS), ModelType.BOOLEAN, true, false, MeasurementUnit.NONE);

    static SimpleAttributeDefinition QUERY_TIMEOUT = new SimpleAttributeDefinition(QUERYTIMEOUT_NAME, TimeOut.Tag.QUERY_TIMEOUT.getLocalName(), new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_TRY_LOCK = new SimpleAttributeDefinition(USETRYLOCK_NAME, TimeOut.Tag.USE_TRY_LOCK.getLocalName(), new ModelNode(), ModelType.LONG, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SET_TX_QUERY_TIMEOUT = new SimpleAttributeDefinition(SETTXQUERYTIMEOUT_NAME, TimeOut.Tag.SET_TX_QUERY_TIMEOUT.getLocalName(), new ModelNode().set(Defaults.SET_TX_QUERY_TIMEOUT), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition TRANSACTION_ISOLATION = new SimpleAttributeDefinition(TRANSACTION_ISOLATION_NAME, DataSource.Tag.TRANSACTION_ISOLATION.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition CHECK_VALID_CONNECTION_SQL = new SimpleAttributeDefinition(CHECKVALIDCONNECTIONSQL_NAME, Validation.Tag.CHECK_VALID_CONNECTION_SQL.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition EXCEPTION_SORTER_CLASSNAME = new SimpleAttributeDefinition(EXCEPTIONSORTERCLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static PropertiesAttributeDefinition EXCEPTION_SORTER_PROPERTIES = new PropertiesAttributeDefinition.Builder(EXCEPTIONSORTER_PROPERTIES_NAME, true)
            .setXmlName(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName())
            .setAllowExpression(true)
            .build();


    static SimpleAttributeDefinition STALE_CONNECTION_CHECKER_CLASSNAME = new SimpleAttributeDefinition(STALECONNECTIONCHECKERCLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static PropertiesAttributeDefinition STALE_CONNECTION_CHECKER_PROPERTIES = new PropertiesAttributeDefinition.Builder(STALECONNECTIONCHECKER_PROPERTIES_NAME, true)
            .setXmlName(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    static SimpleAttributeDefinition VALID_CONNECTION_CHECKER_CLASSNAME = new SimpleAttributeDefinition(VALID_CONNECTION_CHECKER_CLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static PropertiesAttributeDefinition VALID_CONNECTION_CHECKER_PROPERTIES = new PropertiesAttributeDefinition.Builder(VALIDCONNECTIONCHECKER_PROPERTIES_NAME, true)
            .setXmlName(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    static SimpleAttributeDefinition VALIDATE_ON_MATCH = new SimpleAttributeDefinition(VALIDATEONMATCH_NAME, Validation.Tag.VALIDATE_ON_MATCH.getLocalName(), new ModelNode().set(Defaults.VALIDATE_ON_MATCH), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SPY = new SimpleAttributeDefinition(SPY_NAME, DataSource.Attribute.SPY.getLocalName(), new ModelNode().set(Defaults.SPY), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition USE_CCM = new SimpleAttributeDefinition(USE_CCM_NAME, DataSource.Attribute.USE_CCM.getLocalName(), new ModelNode().set(Defaults.USE_CCM), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XA_DATASOURCE_CLASS = new SimpleAttributeDefinition(XADATASOURCECLASS_NAME, XaDataSource.Tag.XA_DATASOURCE_CLASS.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition INTERLEAVING = new SimpleAttributeDefinition(INTERLEAVING_NAME, CommonXaPool.Tag.INTERLEAVING.getLocalName(), new ModelNode().set(Defaults.INTERLEAVING), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition NO_TX_SEPARATE_POOL = new SimpleAttributeDefinition(NOTXSEPARATEPOOL_NAME, CommonXaPool.Tag.NO_TX_SEPARATE_POOLS.getLocalName(), new ModelNode().set(Defaults.NO_TX_SEPARATE_POOL), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition PAD_XID = new SimpleAttributeDefinition(PAD_XID_NAME, CommonXaPool.Tag.PAD_XID.getLocalName(), new ModelNode().set(Defaults.PAD_XID), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition SAME_RM_OVERRIDE = new SimpleAttributeDefinition(SAME_RM_OVERRIDE_NAME, CommonXaPool.Tag.IS_SAME_RM_OVERRIDE.getLocalName(), new ModelNode(), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition WRAP_XA_RESOURCE = new SimpleAttributeDefinition(WRAP_XA_RESOURCE_NAME, CommonXaPool.Tag.WRAP_XA_RESOURCE.getLocalName(), new ModelNode().set(Defaults.WRAP_XA_RESOURCE), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XA_RESOURCE_TIMEOUT = new SimpleAttributeDefinition(XA_RESOURCE_TIMEOUT_NAME, TimeOut.Tag.XA_RESOURCE_TIMEOUT.getLocalName(), new ModelNode(), ModelType.INT, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition REAUTH_PLUGIN_CLASSNAME = new SimpleAttributeDefinition(REAUTHPLUGIN_CLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static PropertiesAttributeDefinition REAUTHPLUGIN_PROPERTIES = new PropertiesAttributeDefinition.Builder(REAUTHPLUGIN_PROPERTIES_NAME, true)
            .setXmlName(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();


    static final SimpleAttributeDefinition[] DATASOURCE_ATTRIBUTE = new SimpleAttributeDefinition[]{CONNECTION_URL,
            DRIVER_CLASS, Constants.DATASOURCE_CLASS, JNDI_NAME,
            DATASOURCE_DRIVER,
            NEW_CONNECTION_SQL, URL_DELIMITER,
            URL_SELECTOR_STRATEGY_CLASS_NAME, USE_JAVA_CONTEXT,
            JTA, org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE,
            org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE, org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL, org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN,
            USERNAME, PASSWORD, SECURITY_DOMAIN,
            REAUTH_PLUGIN_CLASSNAME,
            org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY,
            ALLOW_MULTIPLE_USERS,
            PREPARED_STATEMENTS_CACHE_SIZE,
            SHARE_PREPARED_STATEMENTS,
            TRACK_STATEMENTS,
            ALLOCATION_RETRY,
            ALLOCATION_RETRY_WAIT_MILLIS,
            org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS, org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES,
            QUERY_TIMEOUT,
            USE_TRY_LOCK,
            SET_TX_QUERY_TIMEOUT,
            TRANSACTION_ISOLATION,
            CHECK_VALID_CONNECTION_SQL,
            EXCEPTION_SORTER_CLASSNAME,
            STALE_CONNECTION_CHECKER_CLASSNAME,
            VALID_CONNECTION_CHECKER_CLASSNAME,
            org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS,
            org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION,
            org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL,
            VALIDATE_ON_MATCH, SPY,
            USE_CCM};

    static final PropertiesAttributeDefinition[] DATASOURCE_PROPERTIES_ATTRIBUTES = new PropertiesAttributeDefinition[]{
            REAUTHPLUGIN_PROPERTIES,
            EXCEPTION_SORTER_PROPERTIES,
            STALE_CONNECTION_CHECKER_PROPERTIES,
            VALID_CONNECTION_CHECKER_PROPERTIES,
    };

    static SimpleAttributeDefinition RECOVERY_USERNAME = new SimpleAttributeDefinition(RECOVERY_USERNAME_NAME, Credential.Tag.USER_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_PASSWORD = new SimpleAttributeDefinition(RECOVERY_PASSWORD_NAME, Credential.Tag.PASSWORD.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVERY_SECURITY_DOMAIN = new SimpleAttributeDefinition(RECOVERY_SECURITY_DOMAIN_NAME, Credential.Tag.SECURITY_DOMAIN.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition RECOVER_PLUGIN_CLASSNAME = new SimpleAttributeDefinition(RECOVER_PLUGIN_CLASSNAME_NAME, org.jboss.jca.common.api.metadata.common.Extension.Attribute.CLASS_NAME.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static PropertiesAttributeDefinition RECOVER_PLUGIN_PROPERTIES = new PropertiesAttributeDefinition.Builder(RECOVER_PLUGIN_PROPERTIES_NAME, true)
            .setXmlName(org.jboss.jca.common.api.metadata.common.Extension.Tag.CONFIG_PROPERTY.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();

    static SimpleAttributeDefinition NO_RECOVERY = new SimpleAttributeDefinition(NO_RECOVERY_NAME, Recovery.Attribute.NO_RECOVERY.getLocalName(), new ModelNode(), ModelType.BOOLEAN, true, true, MeasurementUnit.NONE);
    static final SimpleAttributeDefinition[] XA_DATASOURCE_ATTRIBUTE = new SimpleAttributeDefinition[]{
            Constants.XA_DATASOURCE_CLASS, JNDI_NAME, DATASOURCE_DRIVER,
            NEW_CONNECTION_SQL, URL_DELIMITER,
            URL_SELECTOR_STRATEGY_CLASS_NAME, USE_JAVA_CONTEXT,
            org.jboss.as.connector.subsystems.common.pool.Constants.MAX_POOL_SIZE, org.jboss.as.connector.subsystems.common.pool.Constants.MIN_POOL_SIZE,
            org.jboss.as.connector.subsystems.common.pool.Constants.POOL_PREFILL, org.jboss.as.connector.subsystems.common.pool.Constants.POOL_USE_STRICT_MIN, INTERLEAVING,
            NO_TX_SEPARATE_POOL, PAD_XID, SAME_RM_OVERRIDE,
            WRAP_XA_RESOURCE, USERNAME, PASSWORD,
            SECURITY_DOMAIN,
            REAUTH_PLUGIN_CLASSNAME,
            org.jboss.as.connector.subsystems.common.pool.Constants.POOL_FLUSH_STRATEGY, ALLOW_MULTIPLE_USERS,
            PREPARED_STATEMENTS_CACHE_SIZE,
            SHARE_PREPARED_STATEMENTS, TRACK_STATEMENTS,
            ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS,
            org.jboss.as.connector.subsystems.common.pool.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS, org.jboss.as.connector.subsystems.common.pool.Constants.IDLETIMEOUTMINUTES,
            QUERY_TIMEOUT, USE_TRY_LOCK, SET_TX_QUERY_TIMEOUT,
            TRANSACTION_ISOLATION, CHECK_VALID_CONNECTION_SQL,
            EXCEPTION_SORTER_CLASSNAME,
            STALE_CONNECTION_CHECKER_CLASSNAME,
            VALID_CONNECTION_CHECKER_CLASSNAME,
            org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATIONMILLIS,
            org.jboss.as.connector.subsystems.common.pool.Constants.BACKGROUNDVALIDATION,
            org.jboss.as.connector.subsystems.common.pool.Constants.USE_FAST_FAIL,
            VALIDATE_ON_MATCH, XA_RESOURCE_TIMEOUT,
            SPY, USE_CCM,
            RECOVERY_USERNAME, RECOVERY_PASSWORD,
            RECOVERY_SECURITY_DOMAIN, RECOVER_PLUGIN_CLASSNAME,
            NO_RECOVERY, JTA};

    static final PropertiesAttributeDefinition[] XA_DATASOURCE_PROPERTIES_ATTRIBUTES = new PropertiesAttributeDefinition[]{
            REAUTHPLUGIN_PROPERTIES,
            EXCEPTION_SORTER_PROPERTIES,
            STALE_CONNECTION_CHECKER_PROPERTIES,
            VALID_CONNECTION_CHECKER_PROPERTIES,
            RECOVER_PLUGIN_PROPERTIES
    };


    static SimpleAttributeDefinition XADATASOURCE_PROPERTIES = new SimpleAttributeDefinition(XADATASOURCEPROPERTIES_NAME, XaDataSource.Tag.XA_DATASOURCE_PROPERTY.getLocalName(), new ModelNode(), ModelType.STRING, false, true, MeasurementUnit.NONE);

    static SimpleAttributeDefinition XADATASOURCE_PROPERTY_VALUE = new SimpleAttributeDefinition(XADATASOURCEPROPERTIES_VALUE_NAME, XaDataSource.Tag.XA_DATASOURCE_PROPERTY.getLocalName(), new ModelNode(), ModelType.STRING, true, true, MeasurementUnit.NONE);

    static final SimpleAttributeDefinition DRIVER_NAME = new SimpleAttributeDefinitionBuilder(DRIVER_NAME_NAME, ModelType.STRING)
            .setXmlName(Driver.Attribute.NAME.getLocalName())
            .setAllowNull(false)
                    //.setResourceOnly()
            .build();

    static final SimpleAttributeDefinition DRIVER_MODULE_NAME = new SimpleAttributeDefinitionBuilder(DRIVER_MODULE_NAME_NAME, ModelType.STRING)
            .setXmlName(Driver.Attribute.MODULE.getLocalName())
            .setAllowExpression(false)
            .build();

    static final SimpleAttributeDefinition DRIVER_MAJOR_VERSION = new SimpleAttributeDefinitionBuilder(DRIVER_MAJOR_VERSION_NAME, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName(Driver.Attribute.MAJOR_VERSION.getLocalName())
            .build();


    static final SimpleAttributeDefinition DRIVER_MINOR_VERSION = new SimpleAttributeDefinitionBuilder(DRIVER_MINOR_VERSION_NAME, ModelType.INT)
            .setAllowNull(true)
            .setAllowExpression(true)
            .setXmlName(Driver.Attribute.MINOR_VERSION.getLocalName())
            .build();
    static final SimpleAttributeDefinition DRIVER_CLASS_NAME = new SimpleAttributeDefinitionBuilder(DRIVER_CLASS_NAME_NAME, ModelType.STRING)
            .setXmlName(Driver.Tag.DRIVER_CLASS.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(false)
            .build();

    static final SimpleAttributeDefinition DRIVER_DATASOURCE_CLASS_NAME = new SimpleAttributeDefinitionBuilder(DRIVER_DATASOURCE_CLASS_NAME_NAME, ModelType.STRING)
            .setXmlName(Driver.Tag.DATASOURCE_CLASS.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(true)
            .build();


    static final SimpleAttributeDefinition DRIVER_XA_DATASOURCE_CLASS_NAME = new SimpleAttributeDefinitionBuilder(DRIVER_XA_DATASOURCE_CLASS_NAME_NAME, ModelType.STRING)
            .setXmlName(Driver.Tag.XA_DATASOURCE_CLASS.getLocalName())
            .setAllowNull(true)
            .setAllowExpression(false)
            .build();

    static final SimpleAttributeDefinition[] JDBC_DRIVER_ATTRIBUTES = {
            DEPLOYMENT_NAME,
            DRIVER_NAME,
            DRIVER_MODULE_NAME,
            MODULE_SLOT,
            DRIVER_CLASS_NAME,
            DRIVER_DATASOURCE_CLASS_NAME,
            DRIVER_XA_DATASOURCE_CLASS_NAME,
            XA_DATASOURCE_CLASS,
            DRIVER_MAJOR_VERSION,
            DRIVER_MINOR_VERSION,
            JDBC_COMPLIANT
    };


    static final ObjectTypeAttributeDefinition INSTALLED_DRIVER = ObjectTypeAttributeDefinition.Builder.of("installed-driver", JDBC_DRIVER_ATTRIBUTES).build();
    static final ObjectListAttributeDefinition INSTALLED_DRIVERS = ObjectListAttributeDefinition.Builder.of("installed-drivers", INSTALLED_DRIVER)
            .setResourceOnly().setFlags(AttributeAccess.Flag.STORAGE_RUNTIME)
            .build();

    //static final SimpleOperationDefinition INSTALLED_DRIVERS_LIST = new SimpleOperationDefinitionBuilder("installed-drivers-list", DataSourcesExtension.getResourceDescriptionResolver())
    static final SimpleOperationDefinition INSTALLED_DRIVERS_LIST = new SimpleOperationDefinitionBuilder("installed-drivers-list", new NonResolvingResourceDescriptionResolver())
            .setRuntimeOnly()
            .setReplyType(ModelType.LIST)
            .setReplyParameters(JDBC_DRIVER_ATTRIBUTES)
            .build();
    static final SimpleOperationDefinition GET_INSTALLED_DRIVER = new SimpleOperationDefinitionBuilder("get-installed-driver", DataSourcesExtension.getResourceDescriptionResolver())
            .setRuntimeOnly()
            .setParameters(DRIVER_NAME)
            .setReplyParameters(DRIVER_MINOR_VERSION, DRIVER_MAJOR_VERSION, DEPLOYMENT_NAME, DRIVER_NAME, DRIVER_XA_DATASOURCE_CLASS_NAME, XA_DATASOURCE_CLASS, JDBC_COMPLIANT, MODULE_SLOT, DRIVER_CLASS_NAME, DRIVER_MODULE_NAME)
            .setAttributeResolver(DataSourcesExtension.getResourceDescriptionResolver("jdbc-driver"))
            .build();
    static final SimpleOperationDefinition DATASOURCE_ENABLE = new SimpleOperationDefinitionBuilder(ENABLE, DataSourcesExtension.getResourceDescriptionResolver())
            .setParameters(SimpleAttributeDefinitionBuilder.create(PERSISTENT, ModelType.BOOLEAN).setDefaultValue(new ModelNode(true)).build()).build();
    static final SimpleOperationDefinition DATASOURCE_DISABLE = new SimpleOperationDefinitionBuilder(DISABLE, DataSourcesExtension.getResourceDescriptionResolver())
            .build();
    static final SimpleOperationDefinition FLUSH_IDLE_CONNECTION = new SimpleOperationDefinitionBuilder("flush-idle-connection-in-pool", DataSourcesExtension.getResourceDescriptionResolver())
            .setRuntimeOnly().build();
    static final SimpleOperationDefinition FLUSH_ALL_CONNECTION = new SimpleOperationDefinitionBuilder("flush-all-connection-in-pool", DataSourcesExtension.getResourceDescriptionResolver())
            .setRuntimeOnly().build();
    static final SimpleOperationDefinition TEST_CONNECTION = new SimpleOperationDefinitionBuilder("test-connection-in-pool", DataSourcesExtension.getResourceDescriptionResolver())
            .setRuntimeOnly().build();
    static final SimpleOperationDefinition CLEAR_STATISTICS = new SimpleOperationDefinitionBuilder("clear-statistics", DataSourcesExtension.getResourceDescriptionResolver())
            .build();


}
