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
import static org.wildfly.extension.picketlink.idm.Namespace.PICKETLINK_IDENTITY_MANAGEMENT_1_1;
import static org.wildfly.extension.picketlink.idm.Namespace.PICKETLINK_IDENTITY_MANAGEMENT_1_0;

import org.jboss.as.controller.Extension;
import org.jboss.as.controller.ExtensionContext;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.SubsystemRegistration;
import org.jboss.as.controller.descriptions.ResourceDescriptionResolver;
import org.jboss.as.controller.descriptions.DeprecatedResourceDescriptionResolver;
import org.jboss.as.controller.parsing.ExtensionParsingContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker.DiscardAttributeValueChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription.Tools;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.idm.model.IdentityConfigurationResourceDefinition;
import org.wildfly.extension.picketlink.idm.model.LDAPStoreResourceDefinition;
import org.wildfly.extension.picketlink.idm.model.PartitionManagerResourceDefinition;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Silva</a>
 */
public class IDMExtension implements Extension {

    public static final String SUBSYSTEM_NAME = "picketlink-identity-management";
    private static final String RESOURCE_NAME = IDMExtension.class.getPackage().getName() + ".LocalDescriptions";

    private static final ModelVersion CURRENT_MODEL_VERSION = ModelVersion.create(CURRENT.getMajor(), CURRENT.getMinor());

    //deprecated in EAP 6.4
    public static final ModelVersion DEPRECATED_SINCE = ModelVersion.create(2,0,0);

    public static ResourceDescriptionResolver getResourceDescriptionResolver(final String keyPrefix) {
        return new DeprecatedResourceDescriptionResolver(SUBSYSTEM_NAME, keyPrefix, RESOURCE_NAME, IDMExtension.class.getClassLoader(), true, true);
    }

    @Override
    public void initialize(ExtensionContext context) {
        SubsystemRegistration subsystem = context.registerSubsystem(SUBSYSTEM_NAME, CURRENT_MODEL_VERSION);

        subsystem.registerSubsystemModel(IDMSubsystemRootResourceDefinition.INSTANCE);
        subsystem.registerXMLElementWriter(Namespace.CURRENT.getXMLWriter());

        if (context.isRegisterTransformers()) {
            registerTransformers_1_0(context, subsystem);
        }
    }

    private void registerTransformers_1_0(ExtensionContext context, SubsystemRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder partitionManagerResourceBuilder = builder
                .addChildResource(PartitionManagerResourceDefinition.INSTANCE);
        ResourceTransformationDescriptionBuilder identityConfigResourceBuilder = partitionManagerResourceBuilder
                .addChildResource(IdentityConfigurationResourceDefinition.INSTANCE);
        ResourceTransformationDescriptionBuilder ldapTransfDescBuilder = identityConfigResourceBuilder
                .addChildResource(LDAPStoreResourceDefinition.INSTANCE);

        ldapTransfDescBuilder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                LDAPStoreResourceDefinition.ACTIVE_DIRECTORY)
                .setDiscard(new DiscardAttributeValueChecker(new ModelNode(false)),
                        LDAPStoreResourceDefinition.ACTIVE_DIRECTORY);

        ldapTransfDescBuilder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                LDAPStoreResourceDefinition.UNIQUE_ID_ATTRIBUTE_NAME)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, LDAPStoreResourceDefinition.UNIQUE_ID_ATTRIBUTE_NAME);

        Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 0));
    }

    @Override
    public void initializeParsers(ExtensionParsingContext context) {
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, Namespace.CURRENT.getUri(), Namespace.CURRENT::getXMLReader);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PICKETLINK_IDENTITY_MANAGEMENT_1_1.getUri(), PICKETLINK_IDENTITY_MANAGEMENT_1_1::getXMLReader);
        context.setSubsystemXmlMapping(SUBSYSTEM_NAME, PICKETLINK_IDENTITY_MANAGEMENT_1_0.getUri(), PICKETLINK_IDENTITY_MANAGEMENT_1_0::getXMLReader);
    }
}
