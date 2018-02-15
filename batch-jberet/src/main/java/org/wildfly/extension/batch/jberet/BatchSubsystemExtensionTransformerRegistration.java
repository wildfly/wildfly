/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.wildfly.extension.batch.jberet;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;

/**
 * @author <a href="mailto:jperkins@redhat.com">James R. Perkins</a>
 */
public class BatchSubsystemExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    private static final ModelVersion VERSION_1_1_0 = ModelVersion.create(1, 1, 0);

    @Override
    public String getSubsystemName() {
        return BatchSubsystemDefinition.NAME;
    }

    @Override
    public void registerTransformers(final SubsystemTransformerRegistration subsystemRegistration) {
        final ChainedTransformationDescriptionBuilder chainedBuilder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(subsystemRegistration.getCurrentSubsystemVersion());

        chainedBuilder.createBuilder(subsystemRegistration.getCurrentSubsystemVersion(), VERSION_1_1_0)
                .getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.UNDEFINED, BatchSubsystemDefinition.SECURITY_DOMAIN)
                .addRejectCheck(RejectAttributeChecker.DEFINED, BatchSubsystemDefinition.SECURITY_DOMAIN)
                .end();

        chainedBuilder.buildAndRegister(subsystemRegistration, new ModelVersion[]{VERSION_1_1_0});
    }
}