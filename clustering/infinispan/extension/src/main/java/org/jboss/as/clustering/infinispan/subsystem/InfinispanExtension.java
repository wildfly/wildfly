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
package org.jboss.as.clustering.infinispan.subsystem;

import java.util.EnumSet;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.as.controller.transform.description.TransformationDescription;

/**
 * Defines the Infinispan subsystem and its addressable resources.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "infinispan";

    private static final String RESOURCE_NAME = InfinispanExtension.class.getPackage().getName() + ".LocalDescriptions";

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new InfinispanResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, InfinispanExtension.class.getClassLoader());
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initialize(org.jboss.as.controller.ExtensionContext)
     */
    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, InfinispanModel.CURRENT.getVersion());

        // Create the path resolver handler
        ResolvePathHandler resolvePathHandler = context.getProcessType().isServer() ? ResolvePathHandler.Builder.of(context.getPathManager())
                    .setPathAttribute(FileStoreResourceDefinition.RELATIVE_PATH)
                    .setRelativeToAttribute(FileStoreResourceDefinition.RELATIVE_TO)
                    .build() : null;

        registration.registerSubsystemModel(new InfinispanSubsystemResourceDefinition(resolvePathHandler, context.isRuntimeOnlyRegistrationValid()));
        registration.registerXMLElementWriter(new InfinispanSubsystemXMLWriter());

        if (context.isRegisterTransformers()) {
            // Register transformers for all but the current model
            for (InfinispanModel model: EnumSet.complementOf(EnumSet.of(InfinispanModel.CURRENT))) {
                ModelVersion version = model.getVersion();
                TransformationDescription.Tools.register(InfinispanSubsystemResourceDefinition.buildTransformation(version), registration, version);
            }
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (InfinispanSchema schema: InfinispanSchema.values()) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespaceUri(), new InfinispanSubsystemXMLReader(schema));
        }
    }
}
