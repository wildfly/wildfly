/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;

import org.infinispan.partitionhandling.PartitionHandling;
import org.jboss.as.clustering.controller.transform.SimpleAttributeConverter;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker.SimpleRejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Describes resource transformations of the partition handling component of a cache configuration.
 * @author Paul Ferraro
 */
public class PartitionHandlingResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    PartitionHandlingResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(ComponentResourceRegistration.PARTITION_HANDLING.getPathElement());
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, PartitionHandlingResourceDefinitionRegistrar.MERGE_POLICY, PartitionHandlingResourceDefinitionRegistrar.WHEN_SPLIT)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, PartitionHandlingResourceDefinitionRegistrar.MERGE_POLICY)
                    .addRejectCheck(new SimpleRejectAttributeChecker(new ModelNode(PartitionHandling.ALLOW_READS.name())), PartitionHandlingResourceDefinitionRegistrar.WHEN_SPLIT)
                    .setValueConverter(new SimpleAttributeConverter(new SimpleAttributeConverter.Converter() {
                        @Override
                        public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                            if (value.asString(PartitionHandling.ALLOW_READ_WRITES.name()).equals(PartitionHandling.DENY_READ_WRITES.name())) {
                                value.set(ModelNode.TRUE);
                            }
                        }
                    }), PartitionHandlingResourceDefinitionRegistrar.WHEN_SPLIT)
                    .addRename(PartitionHandlingResourceDefinitionRegistrar.WHEN_SPLIT, PartitionHandlingResourceDefinitionRegistrar.ENABLED)
                    .end();
        }
    }
}
