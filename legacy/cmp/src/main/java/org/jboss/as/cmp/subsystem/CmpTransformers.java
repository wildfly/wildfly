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

package org.jboss.as.cmp.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * @author Tomaz Cerar (c) 2017 Red Hat Inc.
 */
public class CmpTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return CmpExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemRegistration) {
        ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();

        builder.addChildResource(CmpSubsystemModel.UUID_KEY_GENERATOR_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .end();
        builder.addChildResource(CmpSubsystemModel.HILO_KEY_GENERATOR_PATH).getAttributeBuilder()
                .addRejectCheck(RejectAttributeChecker.DEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .setDiscard(DiscardAttributeChecker.UNDEFINED, AbstractKeyGeneratorResourceDefinition.JNDI_NAME)
                .end();

        TransformationDescription.Tools.register(builder.build(), subsystemRegistration, ModelVersion.create(1, 0, 0));
    }
}
