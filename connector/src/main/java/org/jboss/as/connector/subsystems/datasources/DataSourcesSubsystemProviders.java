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

import java.util.Locale;
import java.util.ResourceBundle;

import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DEPLOYMENT_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.*;
import static org.jboss.as.connector.subsystems.datasources.Constants.INSTALLED_DRIVERS;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_COMPLIANT;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATA_SOURCE;

import org.jboss.as.connector.pool.PoolConfigurationRWHandler;
import org.jboss.as.connector.pool.PoolMetrics;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.adapters.jdbc.statistics.JdbcStatisticsPlugin;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolStatisticsImpl;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 * @author John Bailey
 */
class DataSourcesSubsystemProviders {

    static final AttributeDefinition[] DATASOURCE_ATTRIBUTE = new AttributeDefinition[] { AttributeDefinition.CONNECTION_URL,
            AttributeDefinition.DRIVER_CLASS, AttributeDefinition.JNDINAME, AttributeDefinition.DRIVER,
            AttributeDefinition.NEW_CONNECTION_SQL, AttributeDefinition.POOLNAME, AttributeDefinition.URL_DELIMITER,
            AttributeDefinition.URL_SELECTOR_STRATEGY_CLASS_NAME, AttributeDefinition.USE_JAVA_CONTEXT,
            AttributeDefinition.ENABLED, AttributeDefinition.JTA, AttributeDefinition.MAX_POOL_SIZE,
            AttributeDefinition.MIN_POOL_SIZE, AttributeDefinition.POOL_PREFILL, AttributeDefinition.POOL_USE_STRICT_MIN,
            AttributeDefinition.USERNAME, AttributeDefinition.PASSWORD, AttributeDefinition.SECURITY_DOMAIN,
            AttributeDefinition.REAUTHPLUGIN_CLASSNAME, AttributeDefinition.REAUTHPLUGIN_PROPERTIES,
            AttributeDefinition.FLUSH_STRATEGY, AttributeDefinition.PREPAREDSTATEMENTSCACHESIZE,
            AttributeDefinition.SHAREPREPAREDSTATEMENTS, AttributeDefinition.TRACKSTATEMENTS,
            AttributeDefinition.ALLOCATION_RETRY, AttributeDefinition.ALLOCATION_RETRY_WAIT_MILLIS,
            AttributeDefinition.BLOCKING_TIMEOUT_WAIT_MILLIS, AttributeDefinition.IDLETIMEOUTMINUTES,
            AttributeDefinition.QUERYTIMEOUT, AttributeDefinition.USETRYLOCK, AttributeDefinition.SETTXQUERYTIMEOUT,
            AttributeDefinition.TRANSACTION_ISOLOATION, AttributeDefinition.CHECKVALIDCONNECTIONSQL,
            AttributeDefinition.EXCEPTIONSORTERCLASSNAME, AttributeDefinition.EXCEPTIONSORTER_PROPERTIES,
            AttributeDefinition.STALECONNECTIONCHECKERCLASSNAME, AttributeDefinition.STALECONNECTIONCHECKER_PROPERTIES,
            AttributeDefinition.VALIDCONNECTIONCHECKERCLASSNAME, AttributeDefinition.VALIDCONNECTIONCHECKER_PROPERTIES,
            AttributeDefinition.BACKGROUNDVALIDATIONMINUTES, AttributeDefinition.BACKGROUNDVALIDATION,
            AttributeDefinition.USE_FAST_FAIL, AttributeDefinition.VALIDATEONMATCH, AttributeDefinition.SPY,
            AttributeDefinition.USE_CCM };

    static final AttributeDefinition[] XA_DATASOURCE_ATTRIBUTE = new AttributeDefinition[] {
            AttributeDefinition.XADATASOURCECLASS, AttributeDefinition.JNDINAME, AttributeDefinition.DRIVER,
            AttributeDefinition.NEW_CONNECTION_SQL, AttributeDefinition.POOLNAME, AttributeDefinition.URL_DELIMITER,
            AttributeDefinition.URL_SELECTOR_STRATEGY_CLASS_NAME, AttributeDefinition.USE_JAVA_CONTEXT,
            AttributeDefinition.ENABLED, AttributeDefinition.MAX_POOL_SIZE, AttributeDefinition.MIN_POOL_SIZE,
            AttributeDefinition.POOL_PREFILL, AttributeDefinition.POOL_USE_STRICT_MIN, AttributeDefinition.INTERLIVING,
            AttributeDefinition.NOTXSEPARATEPOOL, AttributeDefinition.PAD_XID, AttributeDefinition.SAME_RM_OVERRIDE,
            AttributeDefinition.WRAP_XA_DATASOURCE, AttributeDefinition.USERNAME, AttributeDefinition.PASSWORD,
            AttributeDefinition.SECURITY_DOMAIN, AttributeDefinition.RECOVERLUGIN_CLASSNAME,
            AttributeDefinition.REAUTHPLUGIN_CLASSNAME, AttributeDefinition.REAUTHPLUGIN_PROPERTIES,
            AttributeDefinition.FLUSH_STRATEGY, AttributeDefinition.PREPAREDSTATEMENTSCACHESIZE,
            AttributeDefinition.SHAREPREPAREDSTATEMENTS, AttributeDefinition.TRACKSTATEMENTS,
            AttributeDefinition.ALLOCATION_RETRY, AttributeDefinition.ALLOCATION_RETRY_WAIT_MILLIS,
            AttributeDefinition.BLOCKING_TIMEOUT_WAIT_MILLIS, AttributeDefinition.IDLETIMEOUTMINUTES,
            AttributeDefinition.QUERYTIMEOUT, AttributeDefinition.USETRYLOCK, AttributeDefinition.SETTXQUERYTIMEOUT,
            AttributeDefinition.TRANSACTION_ISOLOATION, AttributeDefinition.CHECKVALIDCONNECTIONSQL,
            AttributeDefinition.EXCEPTIONSORTERCLASSNAME, AttributeDefinition.EXCEPTIONSORTER_PROPERTIES,
            AttributeDefinition.STALECONNECTIONCHECKERCLASSNAME, AttributeDefinition.STALECONNECTIONCHECKER_PROPERTIES,
            AttributeDefinition.VALIDCONNECTIONCHECKERCLASSNAME, AttributeDefinition.VALIDCONNECTIONCHECKER_PROPERTIES,
            AttributeDefinition.BACKGROUNDVALIDATIONMINUTES, AttributeDefinition.BACKGROUNDVALIDATION,
            AttributeDefinition.USE_FAST_FAIL, AttributeDefinition.VALIDATEONMATCH, AttributeDefinition.XA_RESOURCE_TIMEOUT,
            AttributeDefinition.SPY, AttributeDefinition.USE_CCM, AttributeDefinition.REAUTHPLUGIN_PROPERTIES,
            AttributeDefinition.RECOVERY_USERNAME, AttributeDefinition.RECOVERY_PASSWORD,
            AttributeDefinition.RECOVERY_SECURITY_DOMAIN, AttributeDefinition.RECOVERLUGIN_CLASSNAME,
            AttributeDefinition.RECOVERLUGIN_PROPERTIES, AttributeDefinition.NO_RECOVERY };

    static final String RESOURCE_NAME = DataSourcesSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final JdbcStatisticsPlugin jdbcMetrics = new JdbcStatisticsPlugin();
    static final ManagedConnectionPoolStatisticsImpl poolMetrics = new ManagedConnectionPoolStatisticsImpl(1);

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();
            subsystem.get(DESCRIPTION).set(bundle.getString("datasources"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.DATASOURCES_1_0.getUriString());

            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, DESCRIPTION).set(bundle.getString("installed-drivers"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, TYPE).set(ModelType.LIST);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DEPLOYMENT_NAME, DESCRIPTION).set(
                    bundle.getString("installed-drivers.deployment-name"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DEPLOYMENT_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DEPLOYMENT_NAME, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DEPLOYMENT_NAME, NILLABLE).set(true);

            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_NAME, DESCRIPTION).set(
                    bundle.getString("installed-drivers.driver-name"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_NAME, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_NAME, NILLABLE).set(true);

            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MODULE_NAME, DESCRIPTION).set(
                    bundle.getString("installed-drivers.module-name"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MODULE_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MODULE_NAME, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MODULE_NAME, NILLABLE).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, MODULE_SLOT, DESCRIPTION).set(
                    bundle.getString("installed-drivers.module-slot"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, MODULE_SLOT, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, MODULE_SLOT, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, MODULE_SLOT, NILLABLE).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("installed-drivers.driver-class"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_CLASS_NAME, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_CLASS_NAME, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_XA_DATASOURCE_CLASS_NAME, DESCRIPTION).set(
                    bundle.getString("installed-drivers.driver-xa-datasource-class-name"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_XA_DATASOURCE_CLASS_NAME, TYPE).set(
                    ModelType.STRING);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_XA_DATASOURCE_CLASS_NAME, REQUIRED).set(true);

            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MAJOR_VERSION, DESCRIPTION).set(
                    bundle.getString("installed-drivers.major-version"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MAJOR_VERSION, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MAJOR_VERSION, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MINOR_VERSION, DESCRIPTION).set(
                    bundle.getString("installed-drivers.minor-version"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MINOR_VERSION, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, DRIVER_MINOR_VERSION, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, JDBC_COMPLIANT, DESCRIPTION).set(
                    bundle.getString("installed-drivers.jdbc-compliant"));
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, JDBC_COMPLIANT, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, INSTALLED_DRIVERS, VALUE_TYPE, JDBC_COMPLIANT, REQUIRED).set(true);

            subsystem.get(OPERATIONS);

            subsystem.get(CHILDREN, JDBC_DRIVER, DESCRIPTION).set(bundle.getString("jdbc-driver"));
            subsystem.get(CHILDREN, JDBC_DRIVER, REQUIRED).set(false);

            subsystem.get(CHILDREN, DATA_SOURCE, DESCRIPTION).set(bundle.getString("data-source"));
            subsystem.get(CHILDREN, DATA_SOURCE, REQUIRED).set(false);

            subsystem.get(CHILDREN, XA_DATA_SOURCE, DESCRIPTION).set(bundle.getString("xa-data-source"));
            subsystem.get(CHILDREN, XA_DATA_SOURCE, REQUIRED).set(false);

            return subsystem;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("datasources.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();

            return operation;
        }
    };

    static final DescriptionProvider INSTALLED_DRIVERS_LIST_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("installed-driver-list");
            operation.get(DESCRIPTION).set(bundle.getString("datasources.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            ModelNode reply = operation.get(REPLY_PROPERTIES);

            reply.get(ATTRIBUTES, DRIVER_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-name"));
            reply.get(ATTRIBUTES, DRIVER_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DEPLOYMENT_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.deployment-name"));
            reply.get(ATTRIBUTES, DEPLOYMENT_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DRIVER_MODULE_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.module-name"));
            reply.get(ATTRIBUTES, DRIVER_MODULE_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, MODULE_SLOT, DESCRIPTION).set(bundle.getString("installed-drivers.module-slot"));
            reply.get(ATTRIBUTES, MODULE_SLOT, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DRIVER_CLASS_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-class"));
            reply.get(ATTRIBUTES, DRIVER_CLASS_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, DESCRIPTION).set(bundle.getString("installed-drivers.major-version"));
            reply.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, TYPE).set(ModelType.INT);
            reply.get(ATTRIBUTES, DRIVER_MINOR_VERSION, DESCRIPTION).set(bundle.getString("installed-drivers.minor-version"));
            reply.get(ATTRIBUTES, DRIVER_MINOR_VERSION, TYPE).set(ModelType.INT);
            reply.get(ATTRIBUTES, JDBC_COMPLIANT, DESCRIPTION).set(bundle.getString("installed-drivers.jdbc-compliant"));
            reply.get(ATTRIBUTES, JDBC_COMPLIANT, TYPE).set(ModelType.BOOLEAN);

            return operation;
        }
    };

    static final DescriptionProvider GET_INSTALLED_DRIVER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("installed-driver-list");
            operation.get(DESCRIPTION).set(bundle.getString("datasources.add"));
            ModelNode request = operation.get(REQUEST_PROPERTIES);
            request.get(ATTRIBUTES, DRIVER_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-name"));
            request.get(ATTRIBUTES, DRIVER_NAME, TYPE).set(ModelType.STRING);
            request.get(ATTRIBUTES, DRIVER_NAME, REQUIRED).set(true);

            ModelNode reply = operation.get(REPLY_PROPERTIES);

            reply.get(ATTRIBUTES, DRIVER_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-name"));
            reply.get(ATTRIBUTES, DRIVER_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DEPLOYMENT_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.deployment-name"));
            reply.get(ATTRIBUTES, DEPLOYMENT_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DRIVER_MODULE_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.module-name"));
            reply.get(ATTRIBUTES, DRIVER_MODULE_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, MODULE_SLOT, DESCRIPTION).set(bundle.getString("installed-drivers.module-slot"));
            reply.get(ATTRIBUTES, MODULE_SLOT, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DRIVER_CLASS_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-class"));
            reply.get(ATTRIBUTES, DRIVER_CLASS_NAME, TYPE).set(ModelType.STRING);
            reply.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, DESCRIPTION).set(bundle.getString("installed-drivers.major-version"));
            reply.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, TYPE).set(ModelType.INT);
            reply.get(ATTRIBUTES, DRIVER_MINOR_VERSION, DESCRIPTION).set(bundle.getString("installed-drivers.minor-version"));
            reply.get(ATTRIBUTES, DRIVER_MINOR_VERSION, TYPE).set(ModelType.INT);
            reply.get(ATTRIBUTES, JDBC_COMPLIANT, DESCRIPTION).set(bundle.getString("installed-drivers.jdbc-compliant"));
            reply.get(ATTRIBUTES, JDBC_COMPLIANT, TYPE).set(ModelType.BOOLEAN);

            return operation;
        }
    };

    static DescriptionProvider JDBC_DRIVER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("jdbc-driver.description"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            node.get(ATTRIBUTES, DRIVER_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-name"));
            node.get(ATTRIBUTES, DRIVER_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, DRIVER_NAME, REQUIRED).set(true);
            node.get(ATTRIBUTES, DEPLOYMENT_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.deployment-name"));
            node.get(ATTRIBUTES, DEPLOYMENT_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, DEPLOYMENT_NAME, REQUIRED).set(false);
            node.get(ATTRIBUTES, DEPLOYMENT_NAME, NILLABLE).set(true);
            node.get(ATTRIBUTES, DRIVER_MODULE_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.module-name"));
            node.get(ATTRIBUTES, DRIVER_MODULE_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, DRIVER_MODULE_NAME, REQUIRED).set(false);
            node.get(ATTRIBUTES, DRIVER_MODULE_NAME, NILLABLE).set(true);

            node.get(ATTRIBUTES, MODULE_SLOT, DESCRIPTION).set(bundle.getString("installed-drivers.module-slot"));
            node.get(ATTRIBUTES, MODULE_SLOT, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, MODULE_SLOT, REQUIRED).set(false);
            node.get(ATTRIBUTES, MODULE_SLOT, NILLABLE).set(true);

            node.get(ATTRIBUTES, DRIVER_CLASS_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-class"));
            node.get(ATTRIBUTES, DRIVER_CLASS_NAME, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, DRIVER_CLASS_NAME, REQUIRED).set(false);
            node.get(ATTRIBUTES, DRIVER_CLASS_NAME, NILLABLE).set(true);

            node.get(ATTRIBUTES, XADATASOURCECLASS, DESCRIPTION).set(
                    bundle.getString("installed-drivers.xa-datasource-class-name"));
            node.get(ATTRIBUTES, XADATASOURCECLASS, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, XADATASOURCECLASS, REQUIRED).set(false);
            node.get(ATTRIBUTES, XADATASOURCECLASS, NILLABLE).set(true);

            node.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, DESCRIPTION).set(bundle.getString("installed-drivers.major-version"));
            node.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, REQUIRED).set(false);
            node.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, NILLABLE).set(true);

            node.get(ATTRIBUTES, DRIVER_MINOR_VERSION, DESCRIPTION).set(bundle.getString("installed-drivers.minor-version"));
            node.get(ATTRIBUTES, DRIVER_MINOR_VERSION, TYPE).set(ModelType.INT);
            node.get(ATTRIBUTES, DRIVER_MINOR_VERSION, REQUIRED).set(false);
            node.get(ATTRIBUTES, DRIVER_MINOR_VERSION, NILLABLE).set(true);

            node.get(ATTRIBUTES, JDBC_COMPLIANT, DESCRIPTION).set(bundle.getString("installed-drivers.jdbc-compliant"));
            node.get(ATTRIBUTES, JDBC_COMPLIANT, TYPE).set(ModelType.BOOLEAN);
            node.get(ATTRIBUTES, JDBC_COMPLIANT, REQUIRED).set(false);
            node.get(ATTRIBUTES, JDBC_COMPLIANT, NILLABLE).set(true);

            return node;
        }
    };

    static DescriptionProvider ADD_JDBC_DRIVER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("jdbc-driver.add"));

            operation.get(ATTRIBUTES, DRIVER_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-name"));
            operation.get(ATTRIBUTES, DRIVER_NAME, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, DRIVER_NAME, REQUIRED).set(true);
            operation.get(ATTRIBUTES, DEPLOYMENT_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.deployment-name"));
            operation.get(ATTRIBUTES, DEPLOYMENT_NAME, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, DRIVER_MODULE_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.module-name"));
            operation.get(ATTRIBUTES, DRIVER_MODULE_NAME, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, MODULE_SLOT, DESCRIPTION).set(bundle.getString("installed-drivers.module-slot"));
            operation.get(ATTRIBUTES, MODULE_SLOT, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, DRIVER_CLASS_NAME, DESCRIPTION).set(bundle.getString("installed-drivers.driver-class"));
            operation.get(ATTRIBUTES, DRIVER_CLASS_NAME, TYPE).set(ModelType.STRING);
            operation.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, DESCRIPTION).set(
                    bundle.getString("installed-drivers.major-version"));
            operation.get(ATTRIBUTES, DRIVER_MAJOR_VERSION, TYPE).set(ModelType.INT);
            operation.get(ATTRIBUTES, DRIVER_MINOR_VERSION, DESCRIPTION).set(
                    bundle.getString("installed-drivers.minor-version"));
            operation.get(ATTRIBUTES, DRIVER_MINOR_VERSION, TYPE).set(ModelType.INT);
            operation.get(ATTRIBUTES, JDBC_COMPLIANT, DESCRIPTION).set(bundle.getString("installed-drivers.jdbc-compliant"));
            operation.get(ATTRIBUTES, JDBC_COMPLIANT, TYPE).set(ModelType.BOOLEAN);

            return operation;
        }
    };

    static DescriptionProvider REMOVE_JDBC_DRIVER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("jdbc-driver.remove"));
            return operation;
        }
    };

    static DescriptionProvider DATA_SOURCE_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("data-source.description"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            for (AttributeDefinition propertyType : DATASOURCE_ATTRIBUTE) {
                node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString(propertyType.getName()));
                node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getModelType());
                node.get(ATTRIBUTES, propertyType.getName(), REQUIRED).set(propertyType.isRequired());
            }

            for (String name : LocalAndXaDataSourcesJdbcMetrics.ATTRIBUTES) {
                node.get(ATTRIBUTES, name, DESCRIPTION).set(jdbcMetrics.getDescription(name));
                ModelType modelType = ModelType.STRING;
                if (jdbcMetrics.getType(name) == int.class) {
                    modelType = ModelType.INT;
                }
                if (jdbcMetrics.getType(name) == long.class) {
                    modelType = ModelType.LONG;
                }
                node.get(ATTRIBUTES, name, TYPE).set(modelType);
                node.get(ATTRIBUTES, name, REQUIRED).set(false);
            }

            for (String name : PoolMetrics.ATTRIBUTES) {
                node.get(ATTRIBUTES, name, DESCRIPTION).set(poolMetrics.getDescription(name));
                ModelType modelType = ModelType.STRING;
                if (poolMetrics.getType(name) == int.class) {
                    modelType = ModelType.INT;
                }
                if (poolMetrics.getType(name) == long.class) {
                    modelType = ModelType.LONG;
                }
                node.get(ATTRIBUTES, name, TYPE).set(modelType);
                node.get(ATTRIBUTES, name, REQUIRED).set(false);
            }

            return node;
        }
    };

    static DescriptionProvider ADD_DATA_SOURCE_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("data-source.add"));

            for (AttributeDefinition propertyType : DATASOURCE_ATTRIBUTE) {
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                        bundle.getString(propertyType.getName()));
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getModelType());
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(propertyType.isRequired());
            }
            return operation;
        }
    };

    static DescriptionProvider REMOVE_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("data-source.remove"));
            return operation;
        }
    };

    static DescriptionProvider ENABLE_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ENABLE);
            operation.get(DESCRIPTION).set(bundle.getString("data-source.enable"));
            return operation;
        }
    };

    static DescriptionProvider DISABLE_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(DISABLE);
            operation.get(DESCRIPTION).set(bundle.getString("data-source.disable"));
            return operation;
        }
    };

    static DescriptionProvider FLUSH_IDLE_CONNECTION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("flush-idle-connection-in-pool");
            operation.get(DESCRIPTION).set(bundle.getString("data-source.flush-idle-connection-in-pool"));
            return operation;
        }
    };

    static DescriptionProvider FLUSH_ALL_CONNECTION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("flush-all-connection-in-pool");
            operation.get(DESCRIPTION).set(bundle.getString("data-source.flush-all-connection-in-pool"));
            return operation;
        }
    };

    static DescriptionProvider TEST_CONNECTION_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set("test-connection-in-pool");
            operation.get(DESCRIPTION).set(bundle.getString("data-source.test-connection-in-pool"));
            return operation;
        }
    };

    static DescriptionProvider XA_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("xa-data-source.description"));
            node.get(HEAD_COMMENT_ALLOWED).set(true);
            node.get(TAIL_COMMENT_ALLOWED).set(true);

            for (AttributeDefinition propertyType : XA_DATASOURCE_ATTRIBUTE) {
                node.get(ATTRIBUTES, propertyType.getName(), DESCRIPTION).set(bundle.getString(propertyType.getName()));
                node.get(ATTRIBUTES, propertyType.getName(), TYPE).set(propertyType.getModelType());
                node.get(ATTRIBUTES, propertyType.getName(), REQUIRED).set(propertyType.isRequired());
            }

            for (String name : LocalAndXaDataSourcesJdbcMetrics.ATTRIBUTES) {
                node.get(ATTRIBUTES, name, DESCRIPTION).set(jdbcMetrics.getDescription(name));
                ModelType modelType = ModelType.STRING;
                if (jdbcMetrics.getType(name) == int.class) {
                    modelType = ModelType.INT;
                }
                if (jdbcMetrics.getType(name) == long.class) {
                    modelType = ModelType.LONG;
                }
                node.get(ATTRIBUTES, name, TYPE).set(modelType);
                node.get(ATTRIBUTES, name, REQUIRED).set(false);
            }
            for (String name : PoolMetrics.ATTRIBUTES) {
                node.get(ATTRIBUTES, name, DESCRIPTION).set(poolMetrics.getDescription(name));
                ModelType modelType = ModelType.STRING;
                if (poolMetrics.getType(name) == int.class) {
                    modelType = ModelType.INT;
                }
                if (poolMetrics.getType(name) == long.class) {
                    modelType = ModelType.LONG;
                }
                node.get(ATTRIBUTES, name, TYPE).set(modelType);
                node.get(ATTRIBUTES, name, REQUIRED).set(false);
            }

            return node;
        }
    };

    static DescriptionProvider ADD_XA_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ADD);
            operation.get(DESCRIPTION).set(bundle.getString("xa-data-source.add"));

            for (AttributeDefinition propertyType : XA_DATASOURCE_ATTRIBUTE) {
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), DESCRIPTION).set(
                        bundle.getString(propertyType.getName()));
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), TYPE).set(propertyType.getModelType());
                operation.get(REQUEST_PROPERTIES, propertyType.getName(), REQUIRED).set(propertyType.isRequired());
            }
            return operation;
        }
    };

    static DescriptionProvider REMOVE_XA_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(REMOVE);
            operation.get(DESCRIPTION).set(bundle.getString("xa-data-source.remove"));
            return operation;
        }
    };

    static DescriptionProvider ENABLE_XA_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(ENABLE);
            operation.get(DESCRIPTION).set(bundle.getString("xa-data-source.enable"));
            return operation;
        }
    };

    static DescriptionProvider DISABLE_XA_DATA_SOURCE_DESC = new DescriptionProvider() {
        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(DISABLE);
            operation.get(DESCRIPTION).set(bundle.getString("xa-data-source.disable"));
            return operation;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }
}
