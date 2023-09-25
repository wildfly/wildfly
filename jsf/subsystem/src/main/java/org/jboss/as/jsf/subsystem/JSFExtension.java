/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.jsf.subsystem;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ParentResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.SubsystemResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.jsf.logging.JSFLogger;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

/**
 * Domain extension used to initialize the Jakarta Server Faces subsystem.
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

    static final ParentResourceDescriptionResolver SUBSYSTEM_RESOLVER = new SubsystemResourceDescriptionResolver(SUBSYSTEM_NAME, JSFExtension.class);

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(1, 1, 0);

    /** {@inheritDoc} */
    @Override
    public void initialize(final ExtensionContext context) {
        JSFLogger.ROOT_LOGGER.debug("Activating JSF(Mojarra) Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerSubsystemModel(new JSFResourceDefinition());
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
            xmlDescription = builder(PATH_SUBSYSTEM, NAMESPACE_1_0)
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
            xmlDescription = builder(PATH_SUBSYSTEM, NAMESPACE_1_1)
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
