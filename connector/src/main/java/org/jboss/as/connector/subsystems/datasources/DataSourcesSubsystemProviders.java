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

import static org.jboss.as.connector.subsystems.datasources.Constants.*;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILDREN;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OPERATION_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author @author <a href="mailto:stefano.maestri@redhat.com">Stefano
 *         Maestri</a>
 */
class DataSourcesSubsystemProviders {

    static final String[] DATASOURCE_ATTRIBUTE = new String[]{CONNECTION_URL, DRIVER_CLASS, JNDINAME, MODULE,
            NEW_CONNECTION_SQL, POOLNAME, URL_DELIMITER, URL_SELECTOR_STRATEGY_CLASS_NAME, USE_JAVA_CONTEXT, ENABLED,
            MAX_POOL_SIZE, MIN_POOL_SIZE, POOL_PREFILL, POOL_USE_STRICT_MIN, USERNAME, PASSWORD, PREPAREDSTATEMENTSCACHESIZE,
            SHAREPREPAREDSTATEMENTS, TRACKSTATEMENTS, ALLOCATION_RETRY, ALLOCATION_RETRY_WAIT_MILLIS,
            BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES, QUERYTIMEOUT, USETRYLOCK, SETTXQUERYTIMEOUT,
            TRANSACTION_ISOLOATION, CHECKVALIDCONNECTIONSQL, EXCEPTIONSORTERCLASSNAME, STALECONNECTIONCHECKERCLASSNAME,
            VALIDCONNECTIONCHECKERCLASSNAME, BACKGROUNDVALIDATIONMINUTES, BACKGROUNDVALIDATION, USE_FAST_FAIL, VALIDATEONMATCH};

    static final String[] XA_DATASOURCE_ATTRIBUTE = new String[]{XADATASOURCECLASS, JNDINAME, MODULE, NEW_CONNECTION_SQL,
            POOLNAME, URL_DELIMITER, URL_SELECTOR_STRATEGY_CLASS_NAME, USE_JAVA_CONTEXT, ENABLED, MAX_POOL_SIZE, MIN_POOL_SIZE,
            POOL_PREFILL, POOL_USE_STRICT_MIN, INTERLIVING, NOTXSEPARATEPOOL, PAD_XID, SAME_RM_OVERRIDE, WRAP_XA_DATASOURCE,
            USERNAME, PASSWORD, PREPAREDSTATEMENTSCACHESIZE, SHAREPREPAREDSTATEMENTS, TRACKSTATEMENTS, ALLOCATION_RETRY,
            ALLOCATION_RETRY_WAIT_MILLIS, BLOCKING_TIMEOUT_WAIT_MILLIS, IDLETIMEOUTMINUTES, QUERYTIMEOUT, USETRYLOCK,
            SETTXQUERYTIMEOUT, TRANSACTION_ISOLOATION, CHECKVALIDCONNECTIONSQL, EXCEPTIONSORTERCLASSNAME,
            STALECONNECTIONCHECKERCLASSNAME, VALIDCONNECTIONCHECKERCLASSNAME, BACKGROUNDVALIDATIONMINUTES,
            BACKGROUNDVALIDATION, USE_FAST_FAIL, VALIDATEONMATCH, XA_RESOURCE_TIMEOUT};

    static final String RESOURCE_NAME = DataSourcesSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            // TODO

            node.get(CHILDREN, JDBC_DRIVER, DESCRIPTION).set(bundle.getString("jdbc.drivers"));
            node.get(CHILDREN, JDBC_DRIVER, REQUIRED).set(false);

            return node;
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD_DESC = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode node = new ModelNode();
            // TODO
            return node;
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

}
