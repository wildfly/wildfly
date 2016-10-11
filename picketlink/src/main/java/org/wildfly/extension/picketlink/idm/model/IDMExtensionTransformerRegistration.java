/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.picketlink.idm.model;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;
import org.wildfly.extension.picketlink.idm.IDMExtension;

/**
 * @author Tomaz Cerar (c) 2016 Red Hat Inc.
 */
public class IDMExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return IDMExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemTransformerRegistration) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
        ResourceTransformationDescriptionBuilder partitionManagerResourceBuilder = builder
                .addChildResource(PartitionManagerResourceDefinition.INSTANCE);
        ResourceTransformationDescriptionBuilder identityConfigResourceBuilder = partitionManagerResourceBuilder
                .addChildResource(IdentityConfigurationResourceDefinition.INSTANCE);
        ResourceTransformationDescriptionBuilder ldapTransfDescBuilder = identityConfigResourceBuilder
                .addChildResource(LDAPStoreResourceDefinition.INSTANCE);

        ldapTransfDescBuilder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                LDAPStoreResourceDefinition.ACTIVE_DIRECTORY)
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)),
                        LDAPStoreResourceDefinition.ACTIVE_DIRECTORY);

        ldapTransfDescBuilder.getAttributeBuilder().addRejectCheck(RejectAttributeChecker.DEFINED,
                LDAPStoreResourceDefinition.UNIQUE_ID_ATTRIBUTE_NAME)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, LDAPStoreResourceDefinition.UNIQUE_ID_ATTRIBUTE_NAME);

        TransformationDescription.Tools.register(builder.build(), subsystemTransformerRegistration, ModelVersion.create(1, 0));
    }
}
