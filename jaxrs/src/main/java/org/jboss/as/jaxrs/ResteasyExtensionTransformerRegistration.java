/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

package org.jboss.as.jaxrs;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
@MetaInfServices
public class ResteasyExtensionTransformerRegistration implements ExtensionTransformerRegistration {

    private static final ModelVersion VERSION_3_0_0 = ModelVersion.create(3, 0, 0);

    @Override
    public String getSubsystemName() {
        return JaxrsExtension.SUBSYSTEM_NAME;
    }

    @Override
    public void registerTransformers(final SubsystemTransformerRegistration subsystemRegistration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        registerV3Transformers(builder.createBuilder(JaxrsExtension.CURRENT_MODEL_VERSION, VERSION_3_0_0));

        builder.buildAndRegister(subsystemRegistration, new ModelVersion[] {VERSION_3_0_0, JaxrsExtension.CURRENT_MODEL_VERSION});
    }

    private static void registerV3Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        subsystem.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD)
                .addRejectCheck(RejectAttributeChecker.DEFINED, JaxrsAttribute.TRACING_TYPE, JaxrsAttribute.TRACING_THRESHOLD);
    }
}
