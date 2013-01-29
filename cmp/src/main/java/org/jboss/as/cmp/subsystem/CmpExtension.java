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

package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * @author John Bailey
 */
public class CmpExtension implements Extension {
    public static final String SUBSYSTEM_NAME = "cmp";

    private static final String RESOURCE_NAME = CmpExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 1;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);

        final ManagementResourceRegistration subsystemRegistration = subsystem.registerSubsystemModel(CMPSubsystemRootResourceDefinition.INSTANCE);
        subsystemRegistration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        subsystem.registerXMLElementWriter(CmpSubsystem11Parser.INSTANCE);

        subsystemRegistration.registerSubModel(UUIDKeyGeneratorResourceDefinition.INSTANCE);

        subsystemRegistration.registerSubModel(HiLoKeyGeneratorResourceDefinition.INSTANCE);
        if (context.isRegisterTransformers()){
            registerTransformers(subsystem);
        }
    }

    private void registerTransformers(SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        builder.addChildResource(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .end();
        builder.addChildResource(CmpSubsystemModel.HILO_KEY_GENERATOR_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .end();

        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 0, 0));

    }

    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CMP_1_0.getUriString(), CmpSubsystem10Parser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CMP_1_1.getUriString(), CmpSubsystem11Parser.INSTANCE);
    }


    public static ResourceDescriptionResolver getResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, CmpExtension.class.getClassLoader(), true, true);
    }

}
