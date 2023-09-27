/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.batch.jberet;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.ExtensionTransformerRegistration;
import org.jboss.as.controller.transform.SubsystemTransformerRegistration;
import org.jboss.as.controller.transform.description.ChainedTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.controller.transform.description.TransformationDescriptionBuilder;
import org.kohsuke.MetaInfServices;
import org.wildfly.extension.batch.jberet.job.repository.CommonAttributes;
import org.wildfly.extension.batch.jberet.job.repository.InMemoryJobRepositoryDefinition;
import org.wildfly.extension.batch.jberet.job.repository.JdbcJobRepositoryDefinition;

@MetaInfServices
public class BatchExtensionTransformerRegistration implements ExtensionTransformerRegistration {
    @Override
    public String getSubsystemName() {
        return BatchSubsystemDefinition.NAME;
    }

    @Override
    public void registerTransformers(SubsystemTransformerRegistration registration) {
        ChainedTransformationDescriptionBuilder builder = TransformationDescriptionBuilder.Factory.createChainedSubystemInstance(registration.getCurrentSubsystemVersion());

        registerV3Transformers(builder.createBuilder(BatchSubsystemExtension.VERSION_3_0_0, BatchSubsystemExtension.VERSION_2_0_0));

        builder.buildAndRegister(registration, new ModelVersion[] {BatchSubsystemExtension.VERSION_1_0_0, BatchSubsystemExtension.VERSION_2_0_0, BatchSubsystemExtension.VERSION_3_0_0});
    }

    private static void registerV3Transformers(ResourceTransformationDescriptionBuilder subsystem) {
        ResourceTransformationDescriptionBuilder inMemoryJobRepository = subsystem.addChildResource(InMemoryJobRepositoryDefinition.PATH);
        rejectAttribute(inMemoryJobRepository, CommonAttributes.EXECUTION_RECORDS_LIMIT);

        ResourceTransformationDescriptionBuilder jdbcJobRepository = subsystem.addChildResource(JdbcJobRepositoryDefinition.PATH);
        rejectAttribute(jdbcJobRepository, CommonAttributes.EXECUTION_RECORDS_LIMIT);
    }

    /**
     * Rejects attribute if it's defined or discard if it has the default value.
     */
    private static void rejectAttribute(ResourceTransformationDescriptionBuilder resource, AttributeDefinition attribute) {
        resource.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, attribute)
                .addRejectCheck(RejectAttributeChecker.DEFINED, attribute);
    }
}
