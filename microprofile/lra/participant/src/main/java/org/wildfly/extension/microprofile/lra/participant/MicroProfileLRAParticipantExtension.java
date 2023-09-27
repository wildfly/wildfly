/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.lra.participant;

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


public class MicroProfileLRAParticipantExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    static final String SUBSYSTEM_NAME = "microprofile-lra-participant";

    private static final MicroProfileLRAParticipantSubsystemModel CURRENT_MODEL = MicroProfileLRAParticipantSubsystemModel.VERSION_1_0_0;

    static final MicroProfileLRAParticipantSubsystemSchema CURRENT_SCHEMA = MicroProfileLRAParticipantSubsystemSchema.VERSION_1_0;

    private final PersistentResourceXMLDescription currentDescription = CURRENT_SCHEMA.getXMLDescription();

    @Override
    public void initialize(ExtensionContext extensionContext) {
        final SubsystemRegistration sr =  extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL.getVersion());
        sr.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
        final ManagementResourceRegistration root = sr.registerSubsystemModel(new MicroProfileLRAParticipantSubsystemDefinition());
        root.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (MicroProfileLRAParticipantSubsystemSchema schema : EnumSet.allOf(MicroProfileLRAParticipantSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == CURRENT_SCHEMA) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}