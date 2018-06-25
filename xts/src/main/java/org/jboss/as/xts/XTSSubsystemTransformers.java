/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2018 Red Hat Inc.
 */
public class XTSSubsystemTransformers implements ExtensionTransformerRegistration {
    static final ModelVersion MODEL_VERSION_EAP64 = ModelVersion.create(1, 0, 0);
    static final ModelVersion MODEL_VERSION_EAP71 = ModelVersion.create(2, 0, 0);

    @Override
    public String getSubsystemName() {
        return XTSExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
        ChainedTransformationDescriptionBuilder chainedBuilder
            = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(XTSExtension.CURRENT_MODEL_VERSION);

        // 3.0.0 --> 2.0.0
        ResourceTransformationDescriptionBuilder builderEap72 = chainedBuilder.createBuilder(XTSExtension.CURRENT_MODEL_VERSION, MODEL_VERSION_EAP71);
        builderEap72.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.DEFINED, XTSSubsystemDefinition.ASYNC_REGISTRATION)
            .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), XTSSubsystemDefinition.ASYNC_REGISTRATION)
            .end();

        // 2.0.0 --> 1.0.0
        ResourceTransformationDescriptionBuilder builderEap70 = chainedBuilder.createBuilder(MODEL_VERSION_EAP71, MODEL_VERSION_EAP64);
        builderEap70.getAttributeBuilder()
            .addRejectCheck(RejectAttributeChecker.DEFINED, XTSSubsystemDefinition.HOST_NAME, XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION)
            .setDiscard(new DiscardAttributeChecker.DiscardAttributeValueChecker(new ModelNode(false)), XTSSubsystemDefinition.DEFAULT_CONTEXT_PROPAGATION)
            .setDiscard(new DiscardAttributeChecker.DefaultDiscardAttributeChecker() {
                   @Override
                   protected boolean isValueDiscardable(PathAddress address, String attributeName, ModelNode attributeValue, TransformationContext context) {
                       return attributeValue.isDefined() && attributeValue.equals(XTSSubsystemDefinition.HOST_NAME.getDefaultValue());
                   }
               }, XTSSubsystemDefinition.HOST_NAME)
            .end();

           chainedBuilder.buildAndRegister(subsystem, new ModelVersion[]{
                   MODEL_VERSION_EAP64,
                   MODEL_VERSION_EAP71,
                   // current is 3.0.0
           });

       }

}
