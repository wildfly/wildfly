/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.microprofile.reactive.streams.operators;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * @author <a href="mailto:kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileReactiveStreamsOperatorsExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-reactive-streams-operators-smallrye";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    static final String WELD_CAPABILITY_NAME = "org.wildfly.weld";

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, MicroProfileReactiveStreamsOperatorsExtension.class);

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;

    private final PersistentResourceXMLDescription currentDescription = MicroProfileReactiveStreamsOperatorsSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(ExtensionContext extensionContext) {
        final SubsystemRegistration sr =  extensionContext.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        sr.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
        final ManagementResourceRegistration root = sr.registerSubsystemModel(new MicroProfileReactiveStreamsOperatorsSubsystemDefinition());
        root.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (MicroProfileReactiveStreamsOperatorsSubsystemSchema schema : EnumSet.allOf(MicroProfileReactiveStreamsOperatorsSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == MicroProfileReactiveStreamsOperatorsSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}
