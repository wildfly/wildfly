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
        if (InfinispanModel.VERSION_16_0_0.requiresTransformation(version)) {
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
