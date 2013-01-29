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

import java.util.List;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.services.path.ResolvePathHandler;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

/**
 * Defines the Infinispan subsystem and its addressable resources.
 *
 * @author Paul Ferraro
 * @author Richard Achmatowicz
 */
public class InfinispanExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "infinispan";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    public static final String RESOURCE_NAME = InfinispanExtension.class.getPackage().getName() + "." +"LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 4;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

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
        // IMPORTANT: Management API version != xsd version! Not all Management API changes result in XSD changes
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        // Create the path resolver handler
        final ResolvePathHandler resolvePathHandler;
        if (context.getProcessType().isServer()) {
            resolvePathHandler = ResolvePathHandler.Builder.of(context.getPathManager())
                    .setPathAttribute(FileStoreResource.PATH)
                    .setRelativeToAttribute(FileStoreResource.RELATIVE_TO)
                    .build();
        } else {
            resolvePathHandler = null;
        }

        subsystem.registerSubsystemModel(new InfinispanSubsystemRootResource(resolvePathHandler));
        subsystem.registerXMLElementWriter(new InfinispanSubsystemXMLWriter());
        if (context.isRegisterTransformers()) {
            InfinispanTransformers.registerTransformers(subsystem);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.as.controller.Extension#initializeParsers(org.jboss.as.controller.parsing.ExtensionParsingContext)
     */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace: Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), reader);
            }
        }
    }
}
