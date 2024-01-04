/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.beanvalidation;

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

/*
 * This class implements the Jakarta Bean Validation extension.
 *
 * @author Eduardo Martins
 */
public class BeanValidationExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "bean-validation";

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, BeanValidationExtension.class);

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    private final PersistentResourceXMLDescription currentDescription = BeanValidationSubsystemSchema.CURRENT.getXMLDescription();

    @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new BeanValidationRootDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
        subsystem.registerXMLElementWriter(new PersistentResourceXMLDescriptionWriter(this.currentDescription));
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        for (BeanValidationSubsystemSchema schema : EnumSet.allOf(BeanValidationSubsystemSchema.class)) {
            XMLElementReader<List<ModelNode>> reader = (schema == BeanValidationSubsystemSchema.CURRENT) ? new PersistentResourceXMLDescriptionReader(this.currentDescription) : schema;
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespace().getUri(), reader);
        }
    }
}
