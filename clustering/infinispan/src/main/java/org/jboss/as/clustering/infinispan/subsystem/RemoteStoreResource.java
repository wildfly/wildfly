/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ObjectListAttributeDefinition;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.OperationDefinition;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleOperationDefinitionBuilder;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * Resource description for the addressable resource /subsystem=infinispan/cache-container=X/cache=Y/remote-store=REMOTE_STORE
 *
 * @author Richard Achmatowicz (c) 2011 Red Hat Inc.
 */
public class RemoteStoreResource extends BaseStoreResource {

    public static final PathElement REMOTE_STORE_PATH = PathElement.pathElement(ModelKeys.REMOTE_STORE, ModelKeys.REMOTE_STORE_NAME);

    // attributes
    static final SimpleAttributeDefinition CACHE =
            new SimpleAttributeDefinitionBuilder(ModelKeys.CACHE, ModelType.STRING, true)
                    .setXmlName(Attribute.CACHE.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .build();
    static final SimpleAttributeDefinition TCP_NO_DELAY =
            new SimpleAttributeDefinitionBuilder(ModelKeys.TCP_NO_DELAY, ModelType.BOOLEAN, true)
                    .setXmlName(Attribute.TCP_NO_DELAY.getLocalName())
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(true))
                    .build();
    static final SimpleAttributeDefinition SOCKET_TIMEOUT =
            new SimpleAttributeDefinitionBuilder(ModelKeys.SOCKET_TIMEOUT, ModelType.LONG, true)
                    .setXmlName(Attribute.SOCKET_TIMEOUT.getLocalName())
                    .setMeasurementUnit(MeasurementUnit.MILLISECONDS)
                    .setAllowExpression(false)
                    .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
                    .setDefaultValue(new ModelNode().set(60000))
                    .build();

    static final SimpleAttributeDefinition OUTBOUND_SOCKET_BINDING = new SimpleAttributeDefinition("outbound-socket-binding", ModelType.STRING, true);

    static final ObjectTypeAttributeDefinition REMOTE_SERVER = ObjectTypeAttributeDefinition.
            Builder.of(ModelKeys.REMOTE_SERVER, OUTBOUND_SOCKET_BINDING).
            setAllowNull(true).
            setSuffix("remote-server").
            build();

    static final ObjectListAttributeDefinition REMOTE_SERVERS = ObjectListAttributeDefinition.Builder.of(ModelKeys.REMOTE_SERVERS, REMOTE_SERVER).
            setAllowNull(true).
            build();

    static final AttributeDefinition[] REMOTE_STORE_ATTRIBUTES = {CACHE, TCP_NO_DELAY, SOCKET_TIMEOUT, REMOTE_SERVERS};

    // operations
    private static final OperationDefinition REMOTE_STORE_ADD_DEFINITION = new SimpleOperationDefinitionBuilder(ADD, InfinispanExtension.getResourceDescriptionResolver(ModelKeys.REMOTE_STORE))
        .setParameters(COMMON_STORE_PARAMETERS)
        .addParameter(CACHE)
        .addParameter(TCP_NO_DELAY)
        .addParameter(SOCKET_TIMEOUT)
        .addParameter(REMOTE_SERVERS)
        .setAttributeResolver(InfinispanExtension.getResourceDescriptionResolver(ModelKeys.REMOTE_STORE))
        .build();

    public RemoteStoreResource() {
        super(REMOTE_STORE_PATH,
                InfinispanExtension.getResourceDescriptionResolver(ModelKeys.REMOTE_STORE),
                CacheConfigOperationHandlers.REMOTE_STORE_ADD,
                ReloadRequiredRemoveStepHandler.INSTANCE);
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
        super.registerAttributes(resourceRegistration);

        // check that we don't need a special handler here?
        final OperationStepHandler writeHandler = new ReloadRequiredWriteAttributeHandler(REMOTE_STORE_ATTRIBUTES);
        for (AttributeDefinition attr : REMOTE_STORE_ATTRIBUTES) {
            resourceRegistration.registerReadWriteAttribute(attr, null, writeHandler);
        }
    }

    @Override
    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
        super.registerOperations(resourceRegistration);
    }

    // override the add operation to provide a custom definition (for the optional PROPERTIES parameter to add())
    @Override
    protected void registerAddOperation(final ManagementResourceRegistration registration, final OperationStepHandler handler, OperationEntry.Flag... flags) {
        registration.registerOperationHandler(REMOTE_STORE_ADD_DEFINITION, handler);
    }
}
