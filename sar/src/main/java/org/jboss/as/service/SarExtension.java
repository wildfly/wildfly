/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.service;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLDescriptionReader;
import org.jboss.as.controller.PersistentResourceXMLDescriptionWriter;
import org.jboss.as.controller.ReloadRequiredRemoveStepHandler;
import org.jboss.as.controller.ResourceDefinition;
import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.capability.RuntimeCapability;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Extension used to enable SAR deployments.
 *
 * @author John Bailey
 * @author Emanuel Muckenhuber
 */
public class SarExtension implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:sar:1.0";
    public static final String SUBSYSTEM_NAME = "sar";

    private static final ParentResourceDescriptionResolver RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, SarExtension.class);
    static final PathElement PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SarExtension.SUBSYSTEM_NAME);

    static final String JMX_CAPABILITY = "org.wildfly.management.jmx";
    static final RuntimeCapability<Void> SAR_CAPABILITY = RuntimeCapability.Builder.of("org.wildfly.sar-deployment")
            .addRequirements(JMX_CAPABILITY)
            .build();

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    private final PersistentResourceXMLDescription currentDescription = SarSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        ResourceDefinition definition = new SimpleResourceDefinition(new SimpleResourceDefinition.Parameters(PATH, RESOLVER)
            .setAddHandler(SarSubsystemAdd.INSTANCE)
            .setRemoveHandler(ReloadRequiredRemoveStepHandler.INSTANCE)
            .setCapabilities(SAR_CAPABILITY));
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(definition);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (SarSubsystemSchema schema : EnumSet.allOf(SarSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == SarSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}
