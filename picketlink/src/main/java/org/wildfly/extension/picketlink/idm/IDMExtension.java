/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm;

import static org.wildfly.extension.picketlink.federation.Namespace.CURRENT;
import static org.wildfly.extension.picketlink.idm.Namespace.PICKETLINK_IDENTITY_MANAGEMENT_1_0;
import static org.wildfly.extension.picketlink.idm.Namespace.PICKETLINK_IDENTITY_MANAGEMENT_1_1;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.wildfly.extension.picketlink.idm.model.parser.IDMSubsystemReader_1_0;
import org.wildfly.extension.picketlink.idm.model.parser.IDMSubsystemReader_2_0;
import org.wildfly.extension.picketlink.idm.model.parser.IDMSubsystemWriter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMExtension extends AbstractLegacyExtension {

    public static final String SUBSYSTEM_NAME = "picketlink-identity-management";
    private static final String RESOURCE_NAME = IDMExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(CURRENT.getMajor(), CURRENT.getMinor());

    //deprecated in EAP 6.4
    public static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(2, 0, 0);

    public IDMExtension() {
        super("org.wildfly.extension.picketlink", SUBSYSTEM_NAME);
    }

    @SuppressWarnings("deprecation")
    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, keyPrefix, RESOURCE_NAME, IDMExtension.class.getClassLoader(), true, true);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        final ManagementResourceRegistration registration = subsystem.registerSubsystemModel(IDMSubsystemRootResourceDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(new IDMSubsystemWriter());

        return Collections.singleton(registration);
    }

    @Override
    protected void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUri(), IDMSubsystemReader_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PICKETLINK_IDENTITY_MANAGEMENT_1_1.getUri(), IDMSubsystemReader_2_0::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PICKETLINK_IDENTITY_MANAGEMENT_1_0.getUri(), IDMSubsystemReader_1_0::new);
    }
}
