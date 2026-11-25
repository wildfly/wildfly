/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.ejb3.subsystem;

import java.io.File;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinition;
import org.jboss.as.controller.SimpleAttributeDefinitionBuilder;
import org.jboss.as.controller.operations.validation.IntRangeValidator;
import org.jboss.as.controller.registry.AttributeAccess;
import org.jboss.as.controller.registry.OperationEntry;
import org.jboss.as.server.ServerEnvironment;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

/**
 * @author Paul Ferraro
 */
@Deprecated
public class FilePassivationStoreResourceDefinition extends LegacyPassivationStoreResourceDefinition {

    // this actually has a dependency on a cache factory using default cache container and cache name "passivation"
    // but no attributes to set requirements for !?
    @Deprecated
    public static final SimpleAttributeDefinition MAX_SIZE = MAX_SIZE_BUILDER.build();
    @Deprecated
    public static final SimpleAttributeDefinition RELATIVE_TO = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.RELATIVE_TO, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.RELATIVE_TO.getLocalName())
            .setDefaultValue(new ModelNode().set(ServerEnvironment.SERVER_DATA_DIR))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;
    @Deprecated
    public static final SimpleAttributeDefinition GROUPS_PATH = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.GROUPS_PATH, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.GROUPS_PATH.getLocalName())
            .setDefaultValue(new ModelNode().set("ejb3" + File.separatorChar + "groups"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;
    @Deprecated
    public static final SimpleAttributeDefinition SESSIONS_PATH = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.SESSIONS_PATH, ModelType.STRING, true)
            .setXmlName(EJB3SubsystemXMLAttribute.SESSIONS_PATH.getLocalName())
            .setDefaultValue(new ModelNode().set("ejb3" + File.separatorChar + "sessions"))
            .setAllowExpression(true)
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;
    @Deprecated
    public static final SimpleAttributeDefinition SUBDIRECTORY_COUNT = new SimpleAttributeDefinitionBuilder(EJB3SubsystemModel.SUBDIRECTORY_COUNT, ModelType.LONG, true)
            .setXmlName(EJB3SubsystemXMLAttribute.SUBDIRECTORY_COUNT.getLocalName())
            .setDefaultValue(new ModelNode().set(100))
            .setAllowExpression(true)
            .setValidator(new IntRangeValidator(1, Integer.MAX_VALUE, true, true))
            .setFlags(AttributeAccess.Flag.RESTART_NONE)
            .setDeprecated(DEPRECATED_VERSION)
            .build()
    ;


    private static final AttributeDefinition[] ATTRIBUTES = { IDLE_TIMEOUT, IDLE_TIMEOUT_UNIT, MAX_SIZE, RELATIVE_TO, GROUPS_PATH, SESSIONS_PATH, SUBDIRECTORY_COUNT };

    private static final FilePassivationStoreAdd ADD = new FilePassivationStoreAdd();
    private static final PassivationStoreRemove REMOVE = new PassivationStoreRemove(ADD);

    FilePassivationStoreResourceDefinition() {
        super(EJB3SubsystemModel.FILE_PASSIVATION_STORE, ADD, REMOVE, OperationEntry.Flag.RESTART_NONE, OperationEntry.Flag.RESTART_RESOURCE_SERVICES, ATTRIBUTES);
    }

}
