/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.config.smallrye;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.operations.common.GenericSubsystemDescribeHandler;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


/**
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 */
public class MicroProfileConfigExtension implements Extension {

    /**
     * The name of our subsystem within the model.
     */
    public static final String SUBSYSTEM_NAME = "microprofile-config-smallrye";

    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);
    protected static final PathElement CONFIG_SOURCE_PATH = PathElement.pathElement("config-source");
    protected static final PathElement CONFIG_SOURCE_PROVIDER_PATH = PathElement.pathElement("config-source-provider");

    private static final String RESOURCE_NAME = MicroProfileConfigExtension.class.getPackage().getName() + ".LocalDescriptions";

    protected static final ModelVersion VERSION_1_0_0 = ModelVersion.create(1, 0, 0);
    private static final ModelVersion CURRENT_MODEL_VERSION = VERSION_1_0_0;

    private static final MicroProfileConfigSubsystemParser_1_0 CURRENT_PARSER = new MicroProfileConfigSubsystemParser_1_0();

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        return getResourceDescriptionResolver(true, keyPrefix);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final boolean useUnprefixedChildTypes, final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder();
        for (String kp : keyPrefix) {
            if (prefix.length() > 0){
                prefix.append('.');
            }
            prefix.append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, MicroProfileConfigExtension.class.getClassLoader(), true, useUnprefixedChildTypes);
    }
    @Override
    public void initialize(ExtensionContext context) {
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerXMLElementWriter(CURRENT_PARSER);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new MicroProfileSubsystemDefinition());
        registration.registerOperationHandler(GenericSubsystemDescribeHandler.DEFINITION, GenericSubsystemDescribeHandler.INSTANCE);

        registration.registerSubModel(new ConfigSourceDefinition());
        registration.registerSubModel(new ConfigSourceProviderDefinition());
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, MicroProfileConfigSubsystemParser_1_0.NAMESPACE, CURRENT_PARSER);
    }

}
