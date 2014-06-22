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
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.StandardResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.as.xts.logging.XtsAsLogger;
import org.jboss.dmr.ModelNode;

/**
 * The web services transactions extension.
 *
 * @author <a href="mailto:adinn@redhat.com">Andrew Dinn</a>
 * @author <a href="mailto:tomaz.cerar@redhat.com">Tomaz Cerar</a>
 */
public class XTSExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "xts";
    private static final XTSSubsystemParser parser = new XTSSubsystemParser();
    protected static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);


    private static final String RESOURCE_NAME = XTSExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final int MANAGEMENT_API_MAJOR_VERSION = 2;
    private static final int MANAGEMENT_API_MINOR_VERSION = 0;
    private static final int MANAGEMENT_API_MICRO_VERSION = 0;

    static StandardResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        String prefix = SUBSYSTEM_NAME + (keyPrefix == null ? "" : "." + keyPrefix);
        return new StandardResourceDescriptionResolver(prefix, RESOURCE_NAME, XTSExtension.class.getClassLoader(), true, false);
    }


    public void initialize(ExtensionContext context) {
        XtsAsLogger.ROOT_LOGGER.debug("Initializing XTS Extension");
        final SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, MANAGEMENT_API_MAJOR_VERSION,
                MANAGEMENT_API_MINOR_VERSION, MANAGEMENT_API_MICRO_VERSION);
        subsystem.registerSubsystemModel(XTSSubsystemDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(parser);

        if (context.isRegisterTransformers()) {
            registerTransformers1x(subsystem);
        }
    }

    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.XTS_1_0.getUriString(), parser);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.XTS_2_0.getUriString(), parser);
    }

    private void registerTransformers1x(SubsystemRegistration subsystem) {
        ResourceTransformationDescriptionBuilder builder =
                TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.getAttributeBuilder().setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
            @Override
            protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                return attributeValue.isDefined() && attributeValue.equals(XTSSubsystemDefinition.HOST_NAME.getDefaultValue());
            }
        }, XTSSubsystemDefinition.HOST_NAME)
        .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION)
        .addRejectCheck(RejectAttributeChecker.DEFINED, XTSSubsystemDefinition.HOST_NAME, XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION)
        .end();

        TransformationDescription.Tools.register(builder.build(), subsystem, ModelVersion.create(1, 1, 0));

    }


}
