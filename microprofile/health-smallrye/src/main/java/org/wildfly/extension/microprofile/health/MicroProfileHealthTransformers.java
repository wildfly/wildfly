/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2019, Red Hat, Inc., and individual contributors
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

package org.wildfly.extension.microprofile.health;

import static org.jboss.as.controller.transform.description.RejectAttributeChecker.DEFINED;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

@MetaInfServices
public class MicroProfileHealthTransformers implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return MicroProfileHealthExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerTransformers_WildFly_24(builder.createBuilder(MicroProfileHealthExtension.VERSION_3_0_0, MicroProfileHealthExtension.VERSION_2_0_0));
        registerTransformers_WildFly_17(builder.createBuilder(MicroProfileHealthExtension.VERSION_2_0_0, MicroProfileHealthExtension.VERSION_1_0_0));

        builder.buildAndRegister(registration, new ModelVersion[] { MicroProfileHealthExtension.VERSION_2_0_0, MicroProfileHealthExtension.VERSION_1_0_0});
    }

    private void registerTransformers_WildFly_24(ResourceTransformationDescriptionBuilder subsystem) {
        rejectDefinedAttributeWithDefaultValue(subsystem, MicroProfileHealthSubsystemDefinition.EMPTY_STARTUP_CHECKS_STATUS);
    }

    private void registerTransformers_WildFly_17(ResourceTransformationDescriptionBuilder subsystem) {
        rejectDefinedAttributeWithDefaultValue(subsystem, MicroProfileHealthSubsystemDefinition.EMPTY_LIVENESS_CHECKS_STATUS);
        rejectDefinedAttributeWithDefaultValue(subsystem, MicroProfileHealthSubsystemDefinition.EMPTY_READINESS_CHECKS_STATUS);
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
