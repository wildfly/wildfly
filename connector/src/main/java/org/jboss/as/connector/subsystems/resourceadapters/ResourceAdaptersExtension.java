/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.as.connector.subsystems.resourceadapters;

import static org.jboss.as.connector.logging.ConnectorLogger.SUBSYSTEM_RA_LOGGER;
import static org.jboss.as.connector.subsystems.resourceadapters.Constants.RESOURCEADAPTERS_NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;

/**
more  * @author <a href="mailto:stefano.maestri@redhat.com">Stefano Maestri</a>
 * @author <a href="mailto:jeff.zhang@jboss.org">Jeff Zhang</a>
 */
public class ResourceAdaptersExtension implements Extension {

    public static final String SUBSYSTEM_NAME = RESOURCEADAPTERS_NAME;
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);


    private static final int MANAGEMENT_API_MAJOR_VERSION = 3;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    private static final String RESOURCE_NAME = ResourceAdaptersExtension.class.getPackage().getName() + ".LocalDescriptions";

    static final SensitivityClassification RA_SECURITY =
            new SensitivityClassification(SUBSYSTEM_NAME, "resource-adapter-security", false, true, true);

    static final SensitiveTargetAccessConstraintDefinition RA_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(RA_SECURITY);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, ResourceAdaptersExtension.class.getClassLoader(), true, false);
    }

    @Override
    public void initialize(final ExtensionContext context) {
        SUBSYSTEM_RA_LOGGER.debugf("Initializing ResourceAdapters Extension");
        // Register the remoting subsystem
        final SubsystemRegistration registration = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);


        registration.registerXMLElementWriter(ResourceAdapterSubsystemParser.INSTANCE);

        // Remoting subsystem description and operation handlers
        registration.registerSubsystemModel(new ResourceAdaptersRootResourceDefinition(context.isRuntimeOnlyRegistrationValid()));

        if (context.isRegisterTransformers()) {
            ResourceAdaptersRootResourceDefinition.registerTransformers(registration);
        }

    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.RESOURCEADAPTERS_1_0.getUriString(), ResourceAdapterSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.RESOURCEADAPTERS_1_1.getUriString(), ResourceAdapterSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.RESOURCEADAPTERS_2_0.getUriString(), ResourceAdapterSubsystemParser.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.RESOURCEADAPTERS_3_0.getUriString(), ResourceAdapterSubsystemParser.INSTANCE);
    }

}
