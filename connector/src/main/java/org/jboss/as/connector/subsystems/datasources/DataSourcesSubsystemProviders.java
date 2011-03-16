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

import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY;
import static org.jboss.as.connector.subsystems.datasources.Constants.ALLOCATION_RETRY_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.BACKGROUNDVALIDATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.BACKGROUNDVALIDATIONMINUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.BLOCKING_TIMEOUT_WAIT_MILLIS;
import static org.jboss.as.connector.subsystems.datasources.Constants.CHECKVALIDCONNECTIONSQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.CONNECTION_URL;
import static org.jboss.as.connector.subsystems.datasources.Constants.DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.DRIVER_CLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.ENABLED;
import static org.jboss.as.connector.subsystems.datasources.Constants.EXCEPTIONSORTERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.IDLETIMEOUTMINUTES;
import static org.jboss.as.connector.subsystems.datasources.Constants.INTERLIVING;
import static org.jboss.as.connector.subsystems.datasources.Constants.JDBC_DRIVER;
import static org.jboss.as.connector.subsystems.datasources.Constants.JNDINAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.MAX_POOL_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.MIN_POOL_SIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.MODULE;
import static org.jboss.as.connector.subsystems.datasources.Constants.NEW_CONNECTION_SQL;
import static org.jboss.as.connector.subsystems.datasources.Constants.NOTXSEPARATEPOOL;
import static org.jboss.as.connector.subsystems.datasources.Constants.PAD_XID;
import static org.jboss.as.connector.subsystems.datasources.Constants.PASSWORD;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOLNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOL_PREFILL;
import static org.jboss.as.connector.subsystems.datasources.Constants.POOL_USE_STRICT_MIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.PREPAREDSTATEMENTSCACHESIZE;
import static org.jboss.as.connector.subsystems.datasources.Constants.QUERYTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SAME_RM_OVERRIDE;
import static org.jboss.as.connector.subsystems.datasources.Constants.SECURITY_DOMAIN;
import static org.jboss.as.connector.subsystems.datasources.Constants.SETTXQUERYTIMEOUT;
import static org.jboss.as.connector.subsystems.datasources.Constants.SHAREPREPAREDSTATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.STALECONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRACKSTATEMENTS;
import static org.jboss.as.connector.subsystems.datasources.Constants.TRANSACTION_ISOLOATION;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_DELIMITER;
import static org.jboss.as.connector.subsystems.datasources.Constants.URL_SELECTOR_STRATEGY_CLASS_NAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USERNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.USETRYLOCK;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_FAST_FAIL;
import static org.jboss.as.connector.subsystems.datasources.Constants.USE_JAVA_CONTEXT;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDATEONMATCH;
import static org.jboss.as.connector.subsystems.datasources.Constants.VALIDCONNECTIONCHECKERCLASSNAME;
import static org.jboss.as.connector.subsystems.datasources.Constants.WRAP_XA_DATASOURCE;
import static org.jboss.as.connector.subsystems.datasources.Constants.XADATASOURCECLASS;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_DATASOURCES;
import static org.jboss.as.connector.subsystems.datasources.Constants.XA_RESOURCE_TIMEOUT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.jboss.logging.Logger;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class DataSourcesSubsystemProviders {

    private static final Logger log = Logger.getLogger("org.jboss.as.datasources");

    static final String[] DATASOURCE_ATTRIBUTE = new String[] { CONNECTION_URL, DRIVER_CLASS, JNDINAME, MODULE,
            NEW_CONNECTION_SQL, POOLNAME, URL_DELIMITER, URL_SELECTOR_STRATEGY_CLASS_NAME, USE_JAVA_CONTEXT, ENABLED,
            MAX_POOL_SIZE, MIN_POOL_SIZE, POOL_PREFILL, POOL_USE_STRICT_MIN, USERNAME, PASSWORD, PREPAREDSTATEMENTSCACHESIZE,
            SHAREPREPAREDSTATEMENTS, TRACKSTATEMENTS, ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS,
            BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES, QUERYTIMEOUT, USETRYLOCK, SETTXQUERYTIMEOUT,
            TRANSACTION_ISOLOATION, CHECKVALIDCONNECTIONSQL, EXCEPTIONSORTERCLASSNAME, STALECONNECTIONCHECKERCLASSNAME,
            VALIDCONNECTIONCHECKERCLASSNAME, BACKGROUNDVALIDATIONMINUTES, BACKGROUNDVALIDATION, USE_FAST_FAIL, VALIDATEONMATCH };

    static final NodeAttribute[] DATASOURCE_NODEATTRIBUTE = new NodeAttribute[] {
            new NodeAttribute(CONNECTION_URL, ModelType.STRING, true), new NodeAttribute(DRIVER_CLASS, ModelType.STRING, true),
            new NodeAttribute(JNDINAME, ModelType.STRING, true), new NodeAttribute(MODULE, ModelType.STRING, true),
            new NodeAttribute(NEW_CONNECTION_SQL, ModelType.STRING, false),
            new NodeAttribute(POOL_PREFILL, ModelType.BOOLEAN, false), new NodeAttribute(POOLNAME, ModelType.STRING, false),
            new NodeAttribute(URL_DELIMITER, ModelType.STRING, false),
            new NodeAttribute(URL_SELECTOR_STRATEGY_CLASS_NAME, ModelType.STRING, false),
            new NodeAttribute(USE_JAVA_CONTEXT, ModelType.BOOLEAN, false),
            new NodeAttribute(ENABLED, ModelType.BOOLEAN, false), new NodeAttribute(MAX_POOL_SIZE, ModelType.INT, false),
            new NodeAttribute(MIN_POOL_SIZE, ModelType.INT, false),
            new NodeAttribute(POOL_USE_STRICT_MIN, ModelType.BOOLEAN, false),
            new NodeAttribute(USERNAME, ModelType.STRING, false), new NodeAttribute(PASSWORD, ModelType.STRING, false),
            new NodeAttribute(SECURITY_DOMAIN, ModelType.STRING, false),
            new NodeAttribute(SHAREPREPAREDSTATEMENTS, ModelType.BOOLEAN, false),
            new NodeAttribute(PREPAREDSTATEMENTSCACHESIZE, ModelType.STRING, false),
            new NodeAttribute(TRACKSTATEMENTS, ModelType.BOOLEAN, false),
            new NodeAttribute(ALLOCATION_RETRY, ModelType.BOOLEAN, false),
            new NodeAttribute(ALLOCATION_RETRY_WAIT_MILLIS, ModelType.LONG, false),
            new NodeAttribute(BLOCKING_TIMEOUT_WAIT_MILLIS, ModelType.LONG, false),
            new NodeAttribute(IDLETIMEOUTMINUTES, ModelType.INT, false),
            new NodeAttribute(QUERYTIMEOUT, ModelType.LONG, false), new NodeAttribute(USETRYLOCK, ModelType.BOOLEAN, false),
            new NodeAttribute(SETTXQUERYTIMEOUT, ModelType.BOOLEAN, false),
            new NodeAttribute(TRANSACTION_ISOLOATION, ModelType.STRING, false),
            new NodeAttribute(CHECKVALIDCONNECTIONSQL, ModelType.STRING, false),

            new NodeAttribute(EXCEPTIONSORTERCLASSNAME, ModelType.STRING, false),
            new NodeAttribute(STALECONNECTIONCHECKERCLASSNAME, ModelType.STRING, false),
            new NodeAttribute(VALIDCONNECTIONCHECKERCLASSNAME, ModelType.STRING, false),
            new NodeAttribute(BACKGROUNDVALIDATIONMINUTES, ModelType.INT, false),
            new NodeAttribute(BACKGROUNDVALIDATION, ModelType.BOOLEAN, false),
            new NodeAttribute(USE_FAST_FAIL, ModelType.BOOLEAN, false),
            new NodeAttribute(VALIDATEONMATCH, ModelType.BOOLEAN, false) };

    static final String[] XA_DATASOURCE_ATTRIBUTE = new String[] { XADATASOURCECLASS, JNDINAME, MODULE, NEW_CONNECTION_SQL,
            POOLNAME, URL_DELIMITER, URL_SELECTOR_STRATEGY_CLASS_NAME, USE_JAVA_CONTEXT, ENABLED, MAX_POOL_SIZE, MIN_POOL_SIZE,
            POOL_PREFILL, POOL_USE_STRICT_MIN, INTERLIVING, NOTXSEPARATEPOOL, PAD_XID, SAME_RM_OVERRIDE, WRAP_XA_DATASOURCE,
            USERNAME, PASSWORD, SECURITY_DOMAIN, PREPAREDSTATEMENTSCACHESIZE, SHAREPREPAREDSTATEMENTS, TRACKSTATEMENTS,
            ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS, BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES, QUERYTIMEOUT,
            USETRYLOCK, SETTXQUERYTIMEOUT, TRANSACTION_ISOLOATION, CHECKVALIDCONNECTIONSQL, EXCEPTIONSORTERCLASSNAME,
            STALECONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKERCLASSNAME, BACKGROUNDVALIDATIONMINUTES,
            BACKGROUNDVALIDATION, USE_FAST_FAIL, VALIDATEONMATCH, XA_RESOURCE_TIMEOUT };

    static final NodeAttribute[] XA_DATASOURCE_NODEATTRIBUTE = new NodeAttribute[] {
            new NodeAttribute(XADATASOURCECLASS, ModelType.STRING, true), new NodeAttribute(JNDINAME, ModelType.STRING, true),
            new NodeAttribute(MODULE, ModelType.STRING, true), new NodeAttribute(NEW_CONNECTION_SQL, ModelType.STRING, false),
            new NodeAttribute(POOLNAME, ModelType.STRING, false), new NodeAttribute(URL_DELIMITER, ModelType.STRING, false),
            new NodeAttribute(URL_SELECTOR_STRATEGY_CLASS_NAME, ModelType.STRING, false),
            new NodeAttribute(USE_JAVA_CONTEXT, ModelType.BOOLEAN, false),
            new NodeAttribute(ENABLED, ModelType.BOOLEAN, false), new NodeAttribute(MAX_POOL_SIZE, ModelType.INT, false),
            new NodeAttribute(MIN_POOL_SIZE, ModelType.INT, false), new NodeAttribute(POOL_PREFILL, ModelType.BOOLEAN, false),
            new NodeAttribute(POOL_USE_STRICT_MIN, ModelType.BOOLEAN, false),
            new NodeAttribute(INTERLIVING, ModelType.BOOLEAN, false),
            new NodeAttribute(NOTXSEPARATEPOOL, ModelType.BOOLEAN, false),
            new NodeAttribute(PAD_XID, ModelType.BOOLEAN, false),
            new NodeAttribute(SAME_RM_OVERRIDE, ModelType.BOOLEAN, false),
            new NodeAttribute(WRAP_XA_DATASOURCE, ModelType.BOOLEAN, false),
            new NodeAttribute(USERNAME, ModelType.STRING, false), new NodeAttribute(PASSWORD, ModelType.STRING, false),
            new NodeAttribute(SECURITY_DOMAIN, ModelType.STRING, false),
            new NodeAttribute(SHAREPREPAREDSTATEMENTS, ModelType.BOOLEAN, false),
            new NodeAttribute(PREPAREDSTATEMENTSCACHESIZE, ModelType.STRING, false),
            new NodeAttribute(TRACKSTATEMENTS, ModelType.BOOLEAN, false),
            new NodeAttribute(ALLOCATION_RETRY, ModelType.BOOLEAN, false),
            new NodeAttribute(ALLOCATION_RETRY_WAIT_MILLIS, ModelType.LONG, false),
            new NodeAttribute(BLOCKING_TIMEOUT_WAIT_MILLIS, ModelType.LONG, false),
            new NodeAttribute(IDLETIMEOUTMINUTES, ModelType.INT, false),
            new NodeAttribute(QUERYTIMEOUT, ModelType.LONG, false), new NodeAttribute(USETRYLOCK, ModelType.BOOLEAN, false),
            new NodeAttribute(SETTXQUERYTIMEOUT, ModelType.BOOLEAN, false),
            new NodeAttribute(TRANSACTION_ISOLOATION, ModelType.STRING, false),
            new NodeAttribute(CHECKVALIDCONNECTIONSQL, ModelType.STRING, false),
            new NodeAttribute(EXCEPTIONSORTERCLASSNAME, ModelType.STRING, false),
            new NodeAttribute(STALECONNECTIONCHECKERCLASSNAME, ModelType.STRING, false),
            new NodeAttribute(VALIDCONNECTIONCHECKERCLASSNAME, ModelType.STRING, false),
            new NodeAttribute(BACKGROUNDVALIDATIONMINUTES, ModelType.INT, false),
            new NodeAttribute(BACKGROUNDVALIDATION, ModelType.BOOLEAN, false),
            new NodeAttribute(USE_FAST_FAIL, ModelType.BOOLEAN, false),
            new NodeAttribute(VALIDATEONMATCH, ModelType.BOOLEAN, false),
            new NodeAttribute(XA_RESOURCE_TIMEOUT, ModelType.LONG, false)

    };

    static final String RESOURCE_NAME = DataSourcesSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            // TODO

            final ModelNode subsystem = new ModelNode();
            subsystem.get(CHILDREN, JDBC_DRIVER, DESCRIPTION).set(bundle.getString("jdbc.drivers"));
            subsystem.get(CHILDREN, JDBC_DRIVER, REQUIRED).set(false);

            subsystem.get(DESCRIPTION).set(bundle.getString("datasources-subsystem"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.DATASOURCES_1_0.getUriString());
            final ModelNode datasourcesNode = new ModelNode();
            datasourcesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            datasourcesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            datasourcesNode.get(DESCRIPTION).set(DATASOURCES);

            for (NodeAttribute attribute : DATASOURCE_NODEATTRIBUTE) {
                datasourcesNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(bundle.getString(attribute.getName()));
                datasourcesNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                datasourcesNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            subsystem.get(DATASOURCES).set(datasourcesNode);
            final ModelNode xaDatasourcesNode = new ModelNode();
            xaDatasourcesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            xaDatasourcesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            datasourcesNode.get(DESCRIPTION).set(XA_DATASOURCES);

            for (NodeAttribute attribute : XA_DATASOURCE_NODEATTRIBUTE) {
                xaDatasourcesNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(bundle.getString(attribute.getName()));
                xaDatasourcesNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                xaDatasourcesNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            subsystem.get(XA_DATASOURCES).set(xaDatasourcesNode);

            return subsystem;

        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();

            operation.get(OPERATION_NAME).set("add");
            operation.get(DESCRIPTION).set(bundle.getString("datasources.add"));
            operation.get(REQUEST_PROPERTIES).setEmptyObject();
            operation.get(REPLY_PROPERTIES).setEmptyObject();
            final ModelNode datasourcesNode = new ModelNode();
            datasourcesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            datasourcesNode.get(TAIL_COMMENT_ALLOWED).set(true);

            for (NodeAttribute attribute : DATASOURCE_NODEATTRIBUTE) {
                datasourcesNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(bundle.getString(attribute.getName()));
                datasourcesNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                datasourcesNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            operation.get(REQUEST_PROPERTIES, DATASOURCES).set(datasourcesNode);

            final ModelNode xaDatasourcesNode = new ModelNode();
            xaDatasourcesNode.get(HEAD_COMMENT_ALLOWED).set(true);
            xaDatasourcesNode.get(TAIL_COMMENT_ALLOWED).set(true);
            for (NodeAttribute attribute : XA_DATASOURCE_NODEATTRIBUTE) {
                xaDatasourcesNode.get(ATTRIBUTES, attribute.getName(), DESCRIPTION).set(bundle.getString(attribute.getName()));
                xaDatasourcesNode.get(ATTRIBUTES, attribute.getName(), TYPE).set(attribute.getModelType());
                xaDatasourcesNode.get(ATTRIBUTES, attribute.getName(), REQUIRED).set(attribute.isRequired());

            }
            operation.get(REQUEST_PROPERTIES, XA_DATASOURCES).set(xaDatasourcesNode);

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

            node.get(ATTRIBUTES, MODULE, DESCRIPTION).set(bundle.getString("jdbc-driver.module"));
            node.get(ATTRIBUTES, MODULE, TYPE).set(ModelType.STRING);
            node.get(ATTRIBUTES, MODULE, REQUIRED).set(true);

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
            operation.get(REQUEST_PROPERTIES, MODULE, DESCRIPTION).set(bundle.getString("jdbc-driver.module"));
            operation.get(REQUEST_PROPERTIES, MODULE, TYPE).set(ModelType.STRING);
            operation.get(REQUEST_PROPERTIES, MODULE, REQUIRED).set(true);
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

    static DescriptionProvider DESCRIBE_JDBC_DRIVER_DESC = new DescriptionProvider() {

        @Override
        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);
            final ModelNode operation = new ModelNode();
            operation.get(OPERATION_NAME).set(DESCRIBE);
            operation.get(DESCRIPTION).set(bundle.getString("jdbc-driver.describe"));
            return operation;
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    static class NodeAttribute {
        private final String name;
        private final ModelType modelType;
        private final boolean required;

        public NodeAttribute(String name, ModelType modelType, boolean required) {
            super();
            this.name = name;
            this.modelType = modelType;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public ModelType getModelType() {
            return modelType;
        }

        public boolean isRequired() {
            return required;
        }

        @Override
        public String toString() {
            return "NodeAttribute [name=" + name + ", modelType=" + modelType + ", required=" + required + "]";
        }

    }
}
