/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Set;
import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.Attribute;
import org.jboss.as.clustering.infinispan.subsystem.CacheContainerResourceDefinition.ListAttribute;
import org.jboss.dmr.ModelNode;

/**
 * Transformer for cache container resources.
 * @author Paul Ferraro
 */
public class CacheContainerResourceTransformer implements Consumer<ModelVersion> {

    private static final Set<String> SERVER_MODULES = Set.of("org.wildfly.clustering.server.infinispan", "org.wildfly.clustering.singleton.server");
    private static final String LEGACY_SERVER_MODULE = "org.wildfly.clustering.server";

    private final ResourceTransformationDescriptionBuilder builder;

    CacheContainerResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(CacheContainerResourceDefinition.WILDCARD_PATH);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                        @Override
                        protected void convertAttribute(PathAddress address, String name, ModelNode modules, TransformationContext context) {
                            if (modules.isDefined()) {
                                // Handle refactoring of org.wildfly.clustering.server module
                                for (ModelNode module : modules.asList()) {
                                    if (SERVER_MODULES.contains(module.asString())) {
                                        module.set(LEGACY_SERVER_MODULE);
                                    }
                                }
                            }
                        }
                    }, ListAttribute.MODULES.getDefinition())
                    .end();
        }
        if (InfinispanSubsystemModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Attribute.MARSHALLER.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(Attribute.MARSHALLER.getDefinition().getDefaultValue()), Attribute.MARSHALLER.getDefinition())
                    .end();
        }

        new ScatteredCacheResourceTransformer(this.builder).accept(version);
        new DistributedCacheResourceTransformer(this.builder).accept(version);
        new ReplicatedCacheResourceTransformer(this.builder).accept(version);
        new InvalidationCacheResourceTransformer(this.builder).accept(version);
        new LocalCacheResourceTransformer(this.builder).accept(version);
    }
}
