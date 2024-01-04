/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.coordinator;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

import java.util.EnumSet;
import java.util.List;

public class MicroProfileLRACoordinatorExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    static final String SUBSYSTEM_NAME = "microprofile-lra-coordinator";

    private static final MicroProfileLRACoordinatorSubsystemModel CURRENT_MODEL = MicroProfileLRACoordinatorSubsystemModel.VERSION_1_0_0;

    static final MicroProfileLRACoordinatorSubsystemSchema CURRENT_SCHEMA = MicroProfileLRACoordinatorSubsystemSchema.VERSION_1_0;

    private final PersistentResourceXMLDescription currentDescription = CURRENT_SCHEMA.getXMLDescription();

    @Override
    public void initialize(ExtensionContext extensionContext) {
        final SubsystemRegistration subsystem = extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL.getVersion());
        subsystem.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new MicroProfileLRACoordinatorSubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (MicroProfileLRACoordinatorSubsystemSchema schema : EnumSet.allOf(MicroProfileLRACoordinatorSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == CURRENT_SCHEMA) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}