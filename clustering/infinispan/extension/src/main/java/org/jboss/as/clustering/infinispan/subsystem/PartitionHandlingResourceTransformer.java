/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import static org.jboss.as.clustering.infinispan.subsystem.PartitionHandlingResourceDefinition.Attribute.MERGE_POLICY;
import static org.jboss.as.clustering.infinispan.subsystem.PartitionHandlingResourceDefinition.Attribute.WHEN_SPLIT;
import static org.jboss.as.clustering.infinispan.subsystem.PartitionHandlingResourceDefinition.DeprecatedAttribute.ENABLED;

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
 * @author Paul Ferraro
 */
public class PartitionHandlingResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    PartitionHandlingResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(PartitionHandlingResourceDefinition.PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, MERGE_POLICY.getDefinition(), WHEN_SPLIT.getDefinition())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, MERGE_POLICY.getDefinition())
                    .addRejectCheck(new SimpleRejectAttributeChecker(new ModelNode(PartitionHandling.ALLOW_READS.name())), WHEN_SPLIT.getDefinition())
                    .setValueConverter(new SimpleAttributeConverter(new SimpleAttributeConverter.Converter() {
                        @Override
                        public void convert(PathAddress address, String name, ModelNode value, ModelNode model, TransformationContext context) {
                            if (value.asString(PartitionHandling.ALLOW_READ_WRITES.name()).equals(PartitionHandling.DENY_READ_WRITES.name())) {
                                value.set(ModelNode.TRUE);
                            }
                        }
                    }), WHEN_SPLIT.getDefinition())
                    .addRename(WHEN_SPLIT.getDefinition(), ENABLED.getName())
                    .end();
        }
    }
}
