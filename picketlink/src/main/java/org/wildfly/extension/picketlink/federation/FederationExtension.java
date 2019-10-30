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

package org.wildfly.extension.picketlink.federation;

import static org.wildfly.extension.picketlink.federation.Namespace.CURRENT;
import static org.wildfly.extension.picketlink.federation.Namespace.PICKETLINK_FEDERATION_1_1;
import static org.wildfly.extension.picketlink.federation.Namespace.PICKETLINK_FEDERATION_1_0;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.wildfly.extension.picketlink.federation.model.FederationResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.keystore.KeyStoreProviderResourceDefinition;
import org.wildfly.extension.picketlink.federation.model.parser.FederationSubsystemWriter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class FederationExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "picketlink-federation";
    public static final PathElement SUBSYSTEM_PATH = PathElement.pathElement(ModelDescriptionConstants.SUBSYSTEM, SUBSYSTEM_NAME);
    private static final String RESOURCE_NAME = FederationExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(CURRENT.getMajor(), CURRENT.getMinor());

    //deprecated in EAP 6.4
    public static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(2,0,0);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, keyPrefix, RESOURCE_NAME, FederationExtension.class.getClassLoader(), true, true);
    }

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystemRegistration = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystemRegistration.registerSubsystemModel(new FederationSubsystemRootResourceDefinition(context));
        subsystemRegistration.registerXMLElementWriter(FederationSubsystemWriter.INSTANCE);

        if (context.isRegisterTransformers()) {
            registerTransformers_1_0(context, subsystemRegistration);
        }
    }

    private void registerTransformers_1_0(ExtensionContext context, SubsystemRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder federationTransfDescBuilder = builder
            .addChildResource(new FederationResourceDefinition(context));
        ResourceTransformationDescriptionBuilder keyStoreTransfDescBuilder = federationTransfDescBuilder
            .addChildResource(KeyStoreProviderResourceDefinition.INSTANCE);

        keyStoreTransfDescBuilder.rejectChildResource(KeyResourceDefinition.INSTANCE.getPathElement());

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 0));
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, CURRENT.getUri(), CURRENT::getXMLReader);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PICKETLINK_FEDERATION_1_1.getUri(), PICKETLINK_FEDERATION_1_1::getXMLReader);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PICKETLINK_FEDERATION_1_0.getUri(), PICKETLINK_FEDERATION_1_0::getXMLReader);
    }
}
