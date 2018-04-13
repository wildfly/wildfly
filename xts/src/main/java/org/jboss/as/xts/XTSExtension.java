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

package org.jboss.as.xts;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.xts.logging.XtsAsLogger;

/**
 * The web services transactions extension.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class XTSExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "xts";
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);

    static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(3, 0, 0);

    private static final String RESOURCE_NAME = XTSExtension.class.getPackage().getName() + ".LocalDescriptions";


    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, XTSExtension.class.getClassLoader(), true, false);
    }


    public void initialize(ExtensionContext context) {
        XtsAsLogger.ROOT_LOGGER.debug("Initializing XTS Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);
        subsystem.registerSubsystemModel(new XTSSubsystemDefinition());
        subsystem.registerXMLElementWriter(new XTSSubsystemParser());
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.XTS_1_0.getUriString(), XTSSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.XTS_2_0.getUriString(), XTSSubsystemParser::new);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.XTS_3_0.getUriString(), XTSSubsystemParser::new);
    }
}
