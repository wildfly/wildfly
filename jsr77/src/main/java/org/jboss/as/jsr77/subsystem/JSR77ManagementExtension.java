/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jsr77.subsystem;


import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.ProcessType;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class JSR77ManagementExtension  implements Extension {

    public static final String NAMESPACE = "urn:jboss:domain:jsr77:1.0";
    public static final String SUBSYSTEM_NAME = "jsr77";
    private static final J2EEManagementSubsystemParser parser = new J2EEManagementSubsystemParser();
    private static final String RESOURCE_NAME = JSR77ManagementExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, JSR77ManagementExtension.class.getClassLoader(), true, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        // Register the root subsystem resource.
        final ManagementResourceRegistration rootResource = subsystem.registerSubsystemModel(new JSR77ManagementRootResource(context.getProcessType() == ProcessType.APPLICATION_CLIENT));

        // Mandatory describe operation
        rootResource.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        subsystem.registerXMLElementWriter(parser);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, NAMESPACE, () -> parser);
    }

    static class J2EEManagementSubsystemParser extends PersistentResourceXMLParser {

        private static PersistentResourceXMLDescription xmlDescription;
        static {
            xmlDescription = PersistentResourceXMLDescription.builder(new JSR77ManagementRootResource(false), JSR77ManagementExtension.NAMESPACE)
                    .build();
        }
        @Override
        public PersistentResourceXMLDescription getParserDescription() {
            return xmlDescription;
        }
    }

}
