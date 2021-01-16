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

package org.wildfly.extension.microprofile.metrics;

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsExtension.SUBSYSTEM_NAME;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsExtension.VERSION_1_0_0;
import static org.wildfly.extension.microprofile.metrics.MicroProfileMetricsSubsystemDefinition.PREFIX;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

public class MicroProfileMetricsTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration subsystemTransformerRegistration) {
        ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemTransformerRegistration.getCurrentSubsystemVersion());

        registerTransformers_EAP_7_2_0(chainedBuilder.createBuilder(subsystemTransformerRegistration.getCurrentSubsystemVersion(), VERSION_1_0_0));

        chainedBuilder.buildAndRegister(subsystemTransformerRegistration, new ModelVersion[]{ VERSION_1_0_0 });

    }

    private void registerTransformers_EAP_7_2_0(ResourceTransformationDescriptionBuilder builder) {
        ResourceTransformationDescriptionBuilder subsystem = builder.addChildResource(MicroProfileMetricsExtension.SUBSYSTEM_PATH);
        rejectDefinedAttributeWithDefaultValue(subsystem, PREFIX);
    }

    /**
     * Reject the attributes if they are defined or discard them if they are undefined or set to their default value.
     */
    private static void rejectDefinedAttributeWithDefaultValue(ResourceTransformationDescriptionBuilder builder, AttributeDefinition... attrs) {
        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, attrs)
                .addRejectCheck(DEFINED, attrs);
    }
}
