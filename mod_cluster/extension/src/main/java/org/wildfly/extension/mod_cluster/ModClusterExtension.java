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

package org.wildfly.extension.mod_cluster;

import static org.wildfly.extension.mod_cluster.ModClusterLogger.ROOT_LOGGER;

import java.util.EnumSet;

import javax.xml.stream.XMLStreamConstants;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.access.constraint.SensitivityClassification;
import org.jboss.as.controller.access.management.SensitiveTargetAccessConstraintDefinition;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.kohsuke.MetaInfServices;

/**
 * Domain extension used to initialize the mod_cluster subsystem element handlers.
 *
 * @author Jean-Frederic Clere
 * @author Tomaz Cerar
 * @author Radoslav Husar
 */
@MetaInfServices(Extension.class)
public class ModClusterExtension implements XMLStreamConstants, Extension {

    public static final String SUBSYSTEM_NAME = "modcluster";

    private static final String RESOURCE_NAME = ModClusterExtension.class.getPackage().getName() + ".LocalDescriptions";

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, ModClusterExtension.class.getClassLoader(), true, false);
    }

    static final SensitivityClassification MOD_CLUSTER_SECURITY =
            new SensitivityClassification(SUBSYSTEM_NAME, "mod_cluster-security", false, true, true);

    static final SensitiveTargetAccessConstraintDefinition MOD_CLUSTER_SECURITY_DEF = new SensitiveTargetAccessConstraintDefinition(MOD_CLUSTER_SECURITY);

    static final SensitivityClassification MOD_CLUSTER_PROXIES =
            new SensitivityClassification(SUBSYSTEM_NAME, "mod_cluster-proxies", false, false, false);

    static final SensitiveTargetAccessConstraintDefinition MOD_CLUSTER_PROXIES_DEF = new SensitiveTargetAccessConstraintDefinition(MOD_CLUSTER_PROXIES);

    @Override
    public void initialize(ExtensionContext context) {
        ROOT_LOGGER.debugf("Activating mod_cluster extension");

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, ModClusterModel.CURRENT.getVersion());
        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(new ModClusterSubsystemResourceDefinition(context.isRuntimeOnlyRegistrationValid()));

        final ManagementResourceRegistration configuration = registration.registerSubModel(new ModClusterConfigResourceDefinition());

        configuration.registerSubModel(new ModClusterSSLResourceDefinition());
        final ManagementResourceRegistration dynamicLoadProvider = configuration.registerSubModel(DynamicLoadProviderDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(LoadMetricDefinition.INSTANCE);
        dynamicLoadProvider.registerSubModel(CustomLoadMetricDefinition.INSTANCE);

        subsystem.registerXMLElementWriter(() -> new ModClusterSubsystemXMLWriter());
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        for (ModClusterSchema schema : EnumSet.allOf(ModClusterSchema.class)) {
            context.setSubsystemXmlMapping(SUBSYSTEM_NAME, schema.getNamespaceUri(), schema.getXMLReaderSupplier());
        }
    }

}
