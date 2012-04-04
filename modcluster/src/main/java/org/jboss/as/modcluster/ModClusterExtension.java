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

package org.jboss.as.modcluster;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLElementReader;

import javax.xml.stream.XMLStreamConstants;
import java.util.List;

import static org.jboss.as.modcluster.ModClusterLogger.ROOT_LOGGER;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 */
public class ModClusterExtension implements XMLStreamConstants, Extension {

    public static final String SUBSYSTEM_NAME = "modcluster";
    private static final int MANAGEMENT_API_MAJOR_VERSION = 1;
    private static final int MANAGEMENT_API_MINOR_VERSION = 2;

    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, ModClusterExtension.SUBSYSTEM_NAME);
    static final PathElement CONFIGURATION_PATH = PathElement.pathElement(CommonAttributes.MOD_CLUSTER_CONFIG, CommonAttributes.CONFIGURATION);
    static final PathElement SSL_CONFIGURATION_PATH = PathElement.pathElement(CommonAttributes.SSL, CommonAttributes.CONFIGURATION);
    static final PathElement DYNAMIC_LOAD_PROVIDER = PathElement.pathElement(CommonAttributes.DYNAMIC_LOAD_PROVIDER, CommonAttributes.CONFIGURATION);
    static final PathElement LOAD_METRIC = PathElement.pathElement(CommonAttributes.LOAD_METRIC);
    static final PathElement CUSTOM_LOAD_METRIC = PathElement.pathElement(CommonAttributes.CUSTOM_LOAD_METRIC);

    private static final String RESOURCE_NAME = ModClusterExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, ModClusterExtension.class.getClassLoader(), true, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debugf("Activating Mod_cluster Extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION, MANAGEMENT_API_MINOR_VERSION);
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new ModClusterDefinition(context.isRuntimeOnlyRegistrationValid()));

        final ManagementResourceRegistration configuration = registration.registerSubModel(new ModClusterConfigResourceDefinition());

        configuration.registerSubModel(new ModClusterSSLResourceDefinition());
        final ManagementResourceRegistration dynamicLoadProvider = configuration.registerSubModel(DynamicLoadProviderDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(LoadMetricDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(CustomLoadMetricDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(new ModClusterSubsystemXMLWriter());
        subsystem.registerSubsystemTransformer(new ModClusterModelTransformer11());
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (Namespace namespace : Namespace.values()) {
            XMLElementReader<List<ModelNode>> reader = namespace.getXMLReader();
            if (reader != null) {
                context.setSubsystemXmlMapping(SUBSYSTEM_NAME, namespace.getUri(), namespace.getXMLReader());
            }
        }
    }
}
