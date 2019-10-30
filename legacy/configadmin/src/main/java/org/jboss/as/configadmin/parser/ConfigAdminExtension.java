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
package org.jboss.as.configadmin.parser;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;

/**
 * Domain extension used to initialize the ConfigAdmin subsystem.
 *
 * @author Thomas.Diesler@jboss.com
 * @author David Bosschaert
 * @author Stuart Douglas
 */
public class ConfigAdminExtension extends AbstractLegacyExtension {

    public static final String SUBSYSTEM_NAME = "configadmin";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);

    private static final ModelVersion MANAGEMENT_API_VERSION = ModelVersion.create(1, 1, 0);

    private static final String RESOURCE_NAME = ConfigAdminExtension.class.getPackage().getName() + ".LocalDescriptions";
    public static final String EXTENSION_NAME = "org.jboss.as.configadmin";

    public ConfigAdminExtension() {
        super(EXTENSION_NAME, SUBSYSTEM_NAME);
    }

    static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new StandardResourceDescriptionResolver(keyPrefix, RESOURCE_NAME, ConfigAdminExtension.class.getClassLoader(), true, true);
    }

    @Override
    public void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.VERSION_1_0.getUriString(), () -> ConfigAdminParser.INSTANCE);
    }

    @Override
    public Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context) {

        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_VERSION);
        ManagementResourceRegistration subsystemRoot = subsystem.registerSubsystemModel(new ConfigAdminRootResource());

        //no need to register transformers as whole extension was deprecated in EAP 6.1 and hasn't changed since, so 1.1.0 in 6.2+ is same as current

        subsystem.registerXMLElementWriter(ConfigAdminParser.INSTANCE);
        return Collections.singleton(subsystemRoot);
    }
}
