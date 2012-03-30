/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.as.txn.subsystem;

import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ReloadRequiredWriteAttributeHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.client.helpers.MeasurementUnit;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.operations.validation.StringLengthValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * {@link ResourceDefinition} for the root resource of the transaction subsystem.
 *
 * @author Brian Stansberry (c) 2011 Red Hat Inc.
 */
public class TransactionSubsystemRootResourceDefinition extends SimpleResourceDefinition {

    //recovery environment
    public static final SimpleAttributeDefinition BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition STATUS_BINDING = new SimpleAttributeDefinitionBuilder(CommonAttributes.STATUS_BINDING, ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.STATUS_BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition RECOVERY_LISTENER = new SimpleAttributeDefinitionBuilder(CommonAttributes.RECOVERY_LISTENER, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RECOVERY_LISTENER.getLocalName())
            .build();

    //core environment
    public static final SimpleAttributeDefinition NODE_IDENTIFIER = new SimpleAttributeDefinitionBuilder(CommonAttributes.NODE_IDENTIFIER, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("1"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_UUID = new SimpleAttributeDefinitionBuilder("process-id-uuid", ModelType.BOOLEAN, false)
            .setAlternatives("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_BINDING = new SimpleAttributeDefinitionBuilder("process-id-socket-binding", ModelType.STRING, false)
            .setValidator(new StringLengthValidator(1, true))
            .setAlternatives("process-id-uuid")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.BINDING.getLocalName())
            .build();

    public static final SimpleAttributeDefinition PROCESS_ID_SOCKET_MAX_PORTS = new SimpleAttributeDefinitionBuilder("process-id-socket-max-ports", ModelType.INT, true)
            .setValidator(new IntRangeValidator(1, true))
            .setDefaultValue(new ModelNode().set(10))
            .setRequires("process-id-socket-binding")
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.SOCKET_PROCESS_ID_MAX_PORTS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    public static final SimpleAttributeDefinition PATH = new SimpleAttributeDefinitionBuilder(ModelDescriptionConstants.PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("var"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .build();

    //coordinator environment
    public static final SimpleAttributeDefinition ENABLE_STATISTICS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_STATISTICS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO should be runtime-changeable
            .setXmlName(Attribute.ENABLE_STATISTICS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition ENABLE_TSM_STATUS = new SimpleAttributeDefinitionBuilder(CommonAttributes.ENABLE_TSM_STATUS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.ENABLE_TSM_STATUS.getLocalName())
            .build();

    public static final SimpleAttributeDefinition DEFAULT_TIMEOUT = new SimpleAttributeDefinitionBuilder(CommonAttributes.DEFAULT_TIMEOUT, ModelType.INT, true)
            .setMeasurementUnit(MeasurementUnit.SECONDS)
            .setDefaultValue(new ModelNode().set(300))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)  // TODO is this runtime-changeable?
            .setXmlName(Attribute.DEFAULT_TIMEOUT.getLocalName())
            .build();

    //object store
    public static final SimpleAttributeDefinition OBJECT_STORE_RELATIVE_TO = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_RELATIVE_TO, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("jboss.server.data.dir"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.RELATIVE_TO.getLocalName())
            .build();
    public static final SimpleAttributeDefinition OBJECT_STORE_PATH = new SimpleAttributeDefinitionBuilder(CommonAttributes.OBJECT_STORE_PATH, ModelType.STRING, true)
            .setDefaultValue(new ModelNode().set("tx-object-store"))
            .setFlags(AttributeAccess.Flag.RESTART_ALL_SERVICES)
            .setXmlName(Attribute.PATH.getLocalName())
            .build();

    public static final SimpleAttributeDefinition JTS = new SimpleAttributeDefinitionBuilder(CommonAttributes.JTS, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)  //I think the use of statics in arjunta will require a JVM restart
            .build();

    public static final SimpleAttributeDefinition USEHORNETQSTORE = new SimpleAttributeDefinitionBuilder(CommonAttributes.USEHORNETQSTORE, ModelType.BOOLEAN, true)
            .setDefaultValue(new ModelNode().set(false))
            .setFlags(AttributeAccess.Flag.RESTART_JVM)
            .build();

    private final boolean registerRuntimeOnly;

    TransactionSubsystemRootResourceDefinition(boolean registerRuntimeOnly) {
        super(PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, TransactionExtension.SUBSYSTEM_NAME),
                TransactionExtension.getResourceDescriptionResolver(TransactionExtension.SUBSYSTEM_NAME),
                TransactionSubsystemAdd.INSTANCE, ReloadRequiredRemoveStepHandler.INSTANCE,
                OperationEntry.Flag.RESTART_ALL_SERVICES, OperationEntry.Flag.RESTART_ALL_SERVICES);
        this.registerRuntimeOnly = registerRuntimeOnly;
    }


    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {

        resourceRegistration.registerReadWriteAttribute(BINDING, null, new ReloadRequiredWriteAttributeHandler(BINDING));
        resourceRegistration.registerReadWriteAttribute(STATUS_BINDING, null, new ReloadRequiredWriteAttributeHandler(STATUS_BINDING));
        resourceRegistration.registerReadWriteAttribute(RECOVERY_LISTENER, null, new ReloadRequiredWriteAttributeHandler(RECOVERY_LISTENER));
        resourceRegistration.registerReadWriteAttribute(NODE_IDENTIFIER, null, new ReloadRequiredWriteAttributeHandler(NODE_IDENTIFIER));
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_UUID, null, new ReloadRequiredWriteAttributeHandler(PROCESS_ID_UUID));
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_SOCKET_BINDING, null, new ReloadRequiredWriteAttributeHandler(PROCESS_ID_SOCKET_BINDING));
        resourceRegistration.registerReadWriteAttribute(PROCESS_ID_SOCKET_MAX_PORTS, null, new ReloadRequiredWriteAttributeHandler(PROCESS_ID_SOCKET_MAX_PORTS));
        resourceRegistration.registerReadWriteAttribute(RELATIVE_TO, null, new ReloadRequiredWriteAttributeHandler(RELATIVE_TO));
        resourceRegistration.registerReadWriteAttribute(PATH, null, new ReloadRequiredWriteAttributeHandler(PATH));
        resourceRegistration.registerReadWriteAttribute(ENABLE_STATISTICS, null, new ReloadRequiredWriteAttributeHandler(ENABLE_STATISTICS));
        resourceRegistration.registerReadWriteAttribute(ENABLE_TSM_STATUS, null, new ReloadRequiredWriteAttributeHandler(ENABLE_TSM_STATUS));
        resourceRegistration.registerReadWriteAttribute(DEFAULT_TIMEOUT, null, new ReloadRequiredWriteAttributeHandler(DEFAULT_TIMEOUT));
        resourceRegistration.registerReadWriteAttribute(OBJECT_STORE_RELATIVE_TO, null, new ReloadRequiredWriteAttributeHandler(OBJECT_STORE_RELATIVE_TO));
        resourceRegistration.registerReadWriteAttribute(OBJECT_STORE_PATH, null, new ReloadRequiredWriteAttributeHandler(OBJECT_STORE_PATH));
        resourceRegistration.registerReadWriteAttribute(JTS, null, new ReloadRequiredWriteAttributeHandler(JTS));
        resourceRegistration.registerReadWriteAttribute(USEHORNETQSTORE, null, new ReloadRequiredWriteAttributeHandler(USEHORNETQSTORE));

        if (registerRuntimeOnly) {
            TxStatsHandler.INSTANCE.registerMetrics(resourceRegistration);
        }
    }
}
