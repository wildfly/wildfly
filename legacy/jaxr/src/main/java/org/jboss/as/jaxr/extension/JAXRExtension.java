/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.jaxr.extension;

import java.util.Collections;
import java.util.Set;

import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.extension.AbstractLegacyExtension;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.registry.ManagementResourceRegistration;
import org.jboss.as.jaxr.extension.JAXRConstants.Namespace;


/**
 * @author Thomas.Diesler@jboss.com
 * @author Kurt Stam
 * @since 26-Oct-2011
 */
public class JAXRExtension extends AbstractLegacyExtension {

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 2, 0);
    static final String SUBSYSTEM_NAME = "jaxr";
    static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    private static final String RESOURCE_NAME = "org.jboss.as.jaxr.LocalDescriptions";

    static ResourceDescriptionResolver getResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JAXRExtension.class.getClassLoader(), true, false);
    }

    public JAXRExtension() {
        super("org.jboss.as.jaxr", SUBSYSTEM_NAME);
    }

    @Override
    protected Set<ManagementResourceRegistration> initializeLegacyModel(ExtensionContext context) {
        SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystemRegistration.registerXMLElementWriter(JAXRSubsystemWriter.INSTANCE);

        ManagementResourceRegistration subsystemRoot = subsystemRegistration.registerSubsystemModel(new JAXRSubsystemRootResource());

        return Collections.singleton(subsystemRoot);
    }

    @Override
    protected void initializeLegacyParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JAXR_1_1.getUriString(), JAXRSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.JAXR_1_0.getUriString(), JAXRSubsystemParser::new);
    }
}
