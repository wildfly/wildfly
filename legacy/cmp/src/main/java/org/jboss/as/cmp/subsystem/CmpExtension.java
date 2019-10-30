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

/**
 * @author John Bailey
 */
public class CmpExtension extends AbstractLegacyExtension {

    public static final String SUBSYSTEM_NAME = "cmp";

    private static final String RESOURCE_NAME = CmpExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 1, 0);
    static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(1,1,0);

    private static final String extensionName = "org.jboss.as.cmp";

    public CmpExtension() {
        super(extensionName, SUBSYSTEM_NAME);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(final ExtensionContext context) {

        final SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystemRegistration.registerXMLElementWriter(new CmpSubsystem11Parser());

        final ManagementResourceRegistration subsystem =
                subsystemRegistration.registerSubsystemModel(CMPSubsystemRootResourceDefinition.INSTANCE);

        subsystem.registerSubModel(UUIDKeyGeneratorResourceDefinition.INSTANCE);

        subsystem.registerSubModel(HiLoKeyGeneratorResourceDefinition.INSTANCE);
        return Collections.singleton(subsystem);
    }

    @Override
    protected void initializeLegacyParsers(final ExtensionParsingContext context) {

        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CMP_1_0.getUriString(), CmpSubsystem10Parser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CMP_1_1.getUriString(), CmpSubsystem11Parser::new);
    }


    public static ResourceDescriptionResolver getResolver(final String keyPrefix) {
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, keyPrefix, RESOURCE_NAME, CmpExtension.class.getClassLoader(), true, true);
    }

}
