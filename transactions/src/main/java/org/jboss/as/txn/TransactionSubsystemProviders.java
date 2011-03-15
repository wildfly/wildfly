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

package org.jboss.as.txn;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ATTRIBUTES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.HEAD_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MIN_LENGTH;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAMESPACE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REPLY_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TAIL_COMMENT_ALLOWED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE_TYPE;
import static org.jboss.as.txn.CommonAttributes.BINDING;
import static org.jboss.as.txn.CommonAttributes.COORDINATOR_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.CORE_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.ENABLE_STATISTICS;
import static org.jboss.as.txn.CommonAttributes.NODE_IDENTIFIER;
import static org.jboss.as.txn.CommonAttributes.RECOVERY_ENVIRONMENT;
import static org.jboss.as.txn.CommonAttributes.STATUS_BINDING;

import java.util.Locale;
import java.util.ResourceBundle;

import org.jboss.as.controller.descriptions.DescriptionProvider;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Emanuel Muckenhuber
 */
class TransactionSubsystemProviders {


    static final String RESOURCE_NAME = TransactionSubsystemProviders.class.getPackage().getName() + ".LocalDescriptions";

    static final DescriptionProvider SUBSYSTEM = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystem(locale);
        }
    };

    static final DescriptionProvider SUBSYSTEM_ADD = new DescriptionProvider() {

        public ModelNode getModelDescription(final Locale locale) {
            return Descriptions.getSubsystemAdd(locale);
        }
    };

    private static ResourceBundle getResourceBundle(Locale locale) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        return ResourceBundle.getBundle(RESOURCE_NAME, locale);
    }

    private static class Descriptions {
        static ModelNode getSubsystem(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode subsystem = new ModelNode();

            subsystem.get(DESCRIPTION).set(bundle.getString("txn"));
            subsystem.get(HEAD_COMMENT_ALLOWED).set(true);
            subsystem.get(TAIL_COMMENT_ALLOWED).set(true);
            subsystem.get(NAMESPACE).set(Namespace.TRANSACTIONS_1_0.getUriString());

            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, DESCRIPTION).set(bundle.getString("core-environment"));
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, TYPE).set(ModelType.OBJECT);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, DESCRIPTION).set(bundle.getString("core-environment.node-identifier"));
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, DEFAULT).set(1);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, DESCRIPTION).set(bundle.getString("core-environment.socket-binding"));
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, MIN_LENGTH).set(1);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, REQUIRED).set(true);

            /* Not currently used
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DESCRIPTION).set(bundle.getString("core-environment.socket-process-id-max-ports"));
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, TYPE).set(ModelType.INT);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DEFAULT).set(10);
            subsystem.get(ATTRIBUTES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, REQUIRED).set(false);
            */

            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, DESCRIPTION).set(bundle.getString("recovery-environment"));
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, TYPE).set(ModelType.OBJECT);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, DESCRIPTION).set(bundle.getString("recovery-environment.socket-binding"));
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, MIN_LENGTH).set(1);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, REQUIRED).set(true);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, DESCRIPTION).set(bundle.getString("recovery-environment.status-socket-binding"));
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, TYPE).set(ModelType.STRING);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, MIN_LENGTH).set(1);
            subsystem.get(ATTRIBUTES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, REQUIRED).set(true);

            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, DESCRIPTION).set(bundle.getString("coordinator-environment"));
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, TYPE).set(ModelType.OBJECT);
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, DESCRIPTION).set(bundle.getString("coordinator-environment.enable-statistics"));
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, DEFAULT).set(true);
            /* Not currently used
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, DESCRIPTION).set(bundle.getString("coordinator-environment.default-timeout"));
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, TYPE).set(ModelType.BOOLEAN);
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, REQUIRED).set(false);
            subsystem.get(ATTRIBUTES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, DEFAULT).set(300);
            */

            //object-store does not seem to be used

            return subsystem;
        }

        static ModelNode getSubsystemAdd(Locale locale) {
            final ResourceBundle bundle = getResourceBundle(locale);

            final ModelNode op = new ModelNode();

            op.get(DESCRIPTION).set(bundle.getString("txn.add"));
            op.get(HEAD_COMMENT_ALLOWED).set(true);
            op.get(TAIL_COMMENT_ALLOWED).set(true);
            op.get(NAMESPACE).set(Namespace.TRANSACTIONS_1_0.getUriString());

            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, DESCRIPTION).set(bundle.getString("core-environment"));
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, DESCRIPTION).set(bundle.getString("core-environment.node-identifier"));
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, DEFAULT).set(1);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, NODE_IDENTIFIER, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, DESCRIPTION).set(bundle.getString("core-environment.socket-binding"));
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, MIN_LENGTH).set(1);
            op.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, BINDING, REQUIRED).set(true);

            /* Not currently used
            subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DESCRIPTION).set(bundle.getString("core-environment.socket-process-id-max-ports"));
            subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, TYPE).set(ModelType.INT);
            subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, DEFAULT).set(10);
            subsystem.get(REQUEST_PROPERTIES, CORE_ENVIRONMENT, VALUE_TYPE, SOCKET_PROCESS_ID_MAX_PORTS, REQUIRED).set(false);
            */

            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, DESCRIPTION).set(bundle.getString("recovery-environment"));
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, DESCRIPTION).set(bundle.getString("recovery-environment.socket-binding"));
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, MIN_LENGTH).set(1);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, BINDING, REQUIRED).set(true);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, DESCRIPTION).set(bundle.getString("recovery-environment.status-socket-binding"));
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, TYPE).set(ModelType.STRING);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, MIN_LENGTH).set(1);
            op.get(REQUEST_PROPERTIES, RECOVERY_ENVIRONMENT, VALUE_TYPE, STATUS_BINDING, REQUIRED).set(true);

            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, DESCRIPTION).set(bundle.getString("coordinator-environment"));
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, TYPE).set(ModelType.OBJECT);
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, DESCRIPTION).set(bundle.getString("coordinator-environment.enable-statistics"));
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, TYPE).set(ModelType.BOOLEAN);
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, ENABLE_STATISTICS, DEFAULT).set(true);
            /* Not currently used
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, DESCRIPTION).set(bundle.getString("coordinator-environment.default-timeout"));
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, TYPE).set(ModelType.INT);
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, REQUIRED).set(false);
            op.get(REQUEST_PROPERTIES, COORDINATOR_ENVIRONMENT, VALUE_TYPE, DEFAULT_TIMEOUT, DEFAULT).set(300);
             */

            //object-store does not seem to be used

            op.get(REPLY_PROPERTIES).setEmptyObject();

            return op;
        }
    }
}
