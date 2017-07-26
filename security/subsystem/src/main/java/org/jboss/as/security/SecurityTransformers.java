/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

package org.jboss.as.security;

import static org.jboss.as.security.Constants.MODULE;
import static org.jboss.as.security.MappingProviderModuleDefinition.PATH_PROVIDER_MODULE;
import static org.jboss.as.security.SecuritySubsystemRootResourceDefinition.INITIALIZE_JACC;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class SecurityTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return SecurityExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        // only register transformers for model version 1.3.0 (EAP 6.2+).
        registerTransformers_1_3_0(subsystemRegistration);
    }

    private void registerTransformers_1_3_0(SubsystemTransformerRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = ResourceTransformationDescriptionBuilder.Factory.createSubsystemInstance();
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_REALM));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_KEY_STORE));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_TRUST_STORE));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_KEY_MANAGER));
        builder.rejectChildResource(PathElement.pathElement(Constants.ELYTRON_TRUST_MANAGER));
        builder.getAttributeBuilder()
                .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(INITIALIZE_JACC.getDefaultValue()), INITIALIZE_JACC)
                .addRejectCheck(RejectAttributeChecker.DEFINED, INITIALIZE_JACC);


        builder
                .addChildResource(SecurityExtension.SECURITY_DOMAIN_PATH)
                .addChildResource(SecurityExtension.PATH_AUDIT_CLASSIC)
                .addChildResource(PATH_PROVIDER_MODULE)
                .getAttributeBuilder()
                    .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(ModuleName.PICKETBOX.getName())), MODULE)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MODULE).end();

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 3, 0));
    }
}
