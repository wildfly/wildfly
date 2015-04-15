/*
 *
 *  JBoss, Home of Professional Open Source.
 *  Copyright 2013, Red Hat, Inc., and individual contributors
 *  as indicated by the @author tags. See the copyright.txt file in the
 *  distribution for a full listing of individual contributors.
 *
 *  This is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU Lesser General Public License as
 *  published by the Free Software Foundation; either version 2.1 of
 *  the License, or (at your option) any later version.
 *
 *  This software is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this software; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.extension.security.manager;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/*
 * This class implements the security manager extension.
 *
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 */
public class SecurityManagerExtension implements Extension {

    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, Constants.SUBSYSTEM_NAME);
    protected static final String RESOURCE_NAME = SecurityManagerExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;
    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 0, 0);

    public static StandardResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(Constants.SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, SecurityManagerExtension.class.getClassLoader(), true, false);
    }

        @Override
    public void initialize(final ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(Constants.SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(SecurityManagerRootDefinition.INSTANCE);
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE, false);
        subsystem.registerXMLElementWriter(SecurityManagerSubsystemParser.INSTANCE);
    }

    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(Constants.SUBSYSTEM_NAME, Namespace.SECURITY_MANAGER_1_0.getUriString(), SecurityManagerSubsystemParser.INSTANCE);
    }
}
