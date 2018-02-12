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
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescription;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * @author Tomaz Cerar (c) 2018 Red Hat Inc.
 */
public class XTSSubsystemTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return "xts";
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystem) {
         ResourceTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createSubsystemInstance();
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
