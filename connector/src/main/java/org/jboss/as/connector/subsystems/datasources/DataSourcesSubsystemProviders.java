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

import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.as.controller.descriptions.OverrideDescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.jca.adapters.jdbc.statistics.JdbcStatisticsPlugin;
import org.jboss.jca.core.connectionmanager.pool.mcp.ManagedConnectionPoolStatisticsImpl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATA_SOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.DEPLOYMENT_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MAJOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MINOR_VERSION;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_MODULE_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_XA_DATASOURCE_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.INSTALLED_DRIVERS;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_COMPLIANT;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE_SLOT;
import static org.jboss.as.connector.subsystems.datasources.Constants.STATISTICS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTIES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCE_PROPERTY_VALUE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DISABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ENABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NILLABLE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATIONS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.PERSISTENT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 * @author John Bailey
 */
class DataSourcesSubsystemProviders {


    static final String RESOURCE_NAME = DataSourcesSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final JdbcStatisticsPlugin jdbcMetrics = new JdbcStatisticsPlugin();
    static final ManagedConnectionPoolStatisticsImpl poolMetrics = new ManagedConnectionPoolStatisticsImpl(1);


    static OverrideDescriptionProvider OVERRIDE_DS_DESC = new OverrideDescriptionProvider() {

        @Override
        public Map<String, ModelNode> getAttributeOverrideDescriptions(Locale locale) {
            return Collections.emptyMap();
        }

        @Override
        public Map<String, ModelNode> getChildTypeOverrideDescriptions(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            Map<String, ModelNode> children = new HashMap<String, ModelNode>();
            ModelNode node = new ModelNode();
            node.get(DESCRIPTION).set(bundle.getString("statistics"));
            children.put(STATISTICS, node);
            return children;
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
            operation.get(REQUEST_PROPERTIES, PERSISTENT, DESCRIPTION).set(
                    bundle.getString(PERSISTENT));
            operation.get(REQUEST_PROPERTIES, PERSISTENT, TYPE).set(ModelType.BOOLEAN);
            operation.get(REQUEST_PROPERTIES, PERSISTENT, REQUIRED).set(false);
            operation.get(REQUEST_PROPERTIES, PERSISTENT, DEFAULT).set(true);
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
