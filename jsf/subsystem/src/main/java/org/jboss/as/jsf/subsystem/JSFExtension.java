/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.jsf.logging.JSFLogger;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Domain extension used to initialize the jsf subsystem.
 *
 * @author Stuart Douglas
 * @author Emanuel Muckenhuber
 * @author Stan Silvert
 */
public class JSFExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "jsf";
    public static final String NAMESPACE_1_0 = "urn:jboss:domain:jsf:1.0";
    public static final String NAMESPACE_1_1 = "urn:jboss:domain:jsf:1.1";

    static final PathElement PATH_SUBSYSTEM = PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME);


    private static final String RESOURCE_NAME = JSFExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 1, 0);

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String... keyPrefix) {
        StringBuilder prefix = new StringBuilder(SUBSYSTEM_NAME);
        for (String kp : keyPrefix) {
            prefix.append('.').append(kp);
        }
        return new StandardResourceDescriptionResolver(prefix.toString(), RESOURCE_NAME, JSFExtension.class.getClassLoader(), true, false);
    }

    /** {@inheritDoc} */
    @Override
    public void initialize(final ExtensionContext context) {
        JSFLogger.ROOT_LOGGER.debug("Activating JSF(Mojarra) Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerSubsystemModel(JSFResourceDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(JSFSubsystemParser_1_1.INSTANCE);
    }

    /** {@inheritDoc} */
    @Override
    public void initializeParsers(final ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JSFExtension.NAMESPACE_1_0, () -> JSFSubsystemParser_1_0.INSTANCE);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, JSFExtension.NAMESPACE_1_1, () -> JSFSubsystemParser_1_1.INSTANCE);
    }

    static class JSFSubsystemParser_1_0 extends PersistentResourceXMLParser {

        private static final JSFSubsystemParser_1_0 INSTANCE = new JSFSubsystemParser_1_0();

        private static final PersistentResourceXMLDescription xmlDescription;

        static {
            xmlDescription = builder(JSFResourceDefinition.INSTANCE.getPathElement(), NAMESPACE_1_0)
                    .addAttributes(JSFResourceDefinition.DEFAULT_JSF_IMPL_SLOT)
                    .build();
        }

        @Override
        public PersistentResourceXMLDescription getParserDescription() {
            return xmlDescription;
        }
    }

    static class JSFSubsystemParser_1_1 extends PersistentResourceXMLParser {

        private static final JSFSubsystemParser_1_1 INSTANCE = new JSFSubsystemParser_1_1();
        private static final PersistentResourceXMLDescription xmlDescription;

        static {
            xmlDescription = builder(JSFResourceDefinition.INSTANCE, NAMESPACE_1_1)
                    .addAttributes(JSFResourceDefinition.DEFAULT_JSF_IMPL_SLOT)
                    .addAttributes(JSFResourceDefinition.DISALLOW_DOCTYPE_DECL)
                    .build();
        }

        @Override
        public PersistentResourceXMLDescription getParserDescription() {
            return xmlDescription;
        }
    }
}
