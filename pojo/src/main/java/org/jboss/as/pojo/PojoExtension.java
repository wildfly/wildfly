/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.pojo;

import java.util.EnumSet;
import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
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
 * POJO extension.
 * Support JBoss5 and JBoss6 pojo configuration model.
 *
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 * @author Emanuel Muckenhuber
 */
public class PojoExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "pojo";

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, PojoExtension.class);

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    private final PersistentResourceXMLDescription currentDescription = PojoSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new PojoResource());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);
        subsystem.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (PojoSubsystemSchema schema : EnumSet.allOf(PojoSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == PojoSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}
