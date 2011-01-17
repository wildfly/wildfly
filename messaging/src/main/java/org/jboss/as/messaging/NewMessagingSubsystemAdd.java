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

package org.jboss.as.messaging;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUEST_PROPERTIES;
import static org.jboss.as.messaging.CommonAttributes.BACKUP;
import static org.jboss.as.messaging.CommonAttributes.BACKUP_CONNECTOR_REF;
import static org.jboss.as.messaging.CommonAttributes.CLUSTERED;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_PASSWORD;
import static org.jboss.as.messaging.CommonAttributes.CLUSTER_USER;
import static org.jboss.as.messaging.CommonAttributes.CONNECTION_TTL_OVERRIDE;
import static org.jboss.as.messaging.CommonAttributes.CONNECTOR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_BINDINGS_DIR;
import static org.jboss.as.messaging.CommonAttributes.CREATE_JOURNAL_DIR;
import org.hornetq.core.config.Configuration;
import org.hornetq.core.config.impl.ConfigurationImpl;
import org.jboss.as.controller.Cancellable;
import org.jboss.as.controller.ModelAddOperationHandler;
import org.jboss.as.controller.NewOperationContext;
import org.jboss.as.controller.ResultHandler;
import org.jboss.as.server.NewRuntimeOperationContext;
import org.jboss.as.server.RuntimeOperationHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceTarget;

/**
 * @author Emanuel Muckenhuber
 */
class NewMessagingSubsystemAdd implements ModelAddOperationHandler, RuntimeOperationHandler {

    /** {@inheritDoc} */
    public Cancellable execute(NewOperationContext context, ModelNode operation, ResultHandler resultHandler) {

        final ModelNode compensatingOperation = new ModelNode();
        compensatingOperation.get(OP).set(REMOVE);
        compensatingOperation.get(OP_ADDR).set(operation.require(OP_ADDR));

        final ModelNode params = operation.get(REQUEST_PROPERTIES);
        final Configuration configuration = transformConfig(params);

        if(context instanceof NewRuntimeOperationContext) {
            final NewRuntimeOperationContext updateContext = (NewRuntimeOperationContext) context;




        }

        return Cancellable.NULL;
    }

    static Configuration transformConfig(final ModelNode params) {
        final Configuration configuration = new ConfigurationImpl();
        // --
        configuration.setBackup(params.get(BACKUP).asBoolean(ConfigurationImpl.DEFAULT_BACKUP));
        if(params.has(BACKUP_CONNECTOR_REF)) {
            configuration.setBackupConnectorName(params.get(BACKUP_CONNECTOR_REF).asString());
        }
        configuration.setClustered(params.get(CLUSTERED).asBoolean(ConfigurationImpl.DEFAULT_CLUSTERED));
        configuration.setClusterPassword(params.get(CLUSTER_PASSWORD).asString());
        configuration.setClusterUser(params.get(CLUSTER_USER).asString());
        configuration.setConnectionTTLOverride(params.get(CONNECTION_TTL_OVERRIDE).asInt((int) ConfigurationImpl.DEFAULT_CONNECTION_TTL_OVERRIDE));
        configuration.setCreateBindingsDir(params.get(CREATE_BINDINGS_DIR).asBoolean(ConfigurationImpl.DEFAULT_CREATE_BINDINGS_DIR));
        configuration.setCreateJournalDir(params.get(CREATE_JOURNAL_DIR).asBoolean(ConfigurationImpl.DEFAULT_CREATE_JOURNAL_DIR));
        // --
        return configuration;
    }

    static void processConnectors(final Configuration configuration, final ModelNode params) {
        if(params.has(CONNECTOR)) {
            for(final Property property : params.get(CONNECTOR).asPropertyList()) {
                final String connectorName = property.getName();
                final ModelNode config = property.getValue();
            }
        }
    }

    static void createDirectoryService(final ModelNode path, final ServiceTarget serviceTarget) {

    }

}
