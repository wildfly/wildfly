/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Transformer for cache container resources.
 * @author Paul Ferraro
 */
public class CacheContainerResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    CacheContainerResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(CacheContainerResourceDefinitionRegistrar.REGISTRATION.getPathElement());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void accept(ModelVersion version) {
        Map<String, String> legacyModules = new TreeMap<>();
        if (InfinispanSubsystemModel.VERSION_19_0_0.requiresTransformation(version)) {
            // Convert wildfly-clustering module to the appropriate module alias
            legacyModules.put("org.wildfly.clustering.session.infinispan.embedded", "org.wildfly.clustering.web.infinispan");
        }
        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(version)) {
            // Handle refactoring of org.wildfly.clustering.server module
            String legacyServerModule = "org.wildfly.clustering.server";
            legacyModules.put("org.wildfly.clustering.server.infinispan", legacyServerModule);
            legacyModules.put("org.wildfly.clustering.singleton.server", legacyServerModule);
        }
        if (InfinispanSubsystemModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, CacheContainerResourceDefinitionRegistrar.MARSHALLER)
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(CacheContainerResourceDefinitionRegistrar.MARSHALLER.getDefaultValue()), CacheContainerResourceDefinitionRegistrar.MARSHALLER)
                    .end();
        }

        if (!legacyModules.isEmpty()) {
            this.builder.getAttributeBuilder().setValueConverter(new AttributeConverter.DefaultAttributeConverter() {
                @Override
                protected void convertAttribute(PathAddress address, String name, ModelNode modules, TransformationContext context) {
                    if (modules.isDefined()) {
                        for (ModelNode module : modules.asList()) {
                            String legacyModule = legacyModules.get(module.asString());
                            if (legacyModule != null) {
                                module.set(legacyModule);
                            }
                        }
                    }
                }
            }, CacheContainerResourceDefinitionRegistrar.MODULES)
            .end();
        }

        new ScatteredCacheResourceTransformer(this.builder).accept(version);
        new DistributedCacheResourceTransformer(this.builder).accept(version);
        new ReplicatedCacheResourceTransformer(this.builder).accept(version);
        new InvalidationCacheResourceTransformer(this.builder).accept(version);
        new LocalCacheResourceTransformer(this.builder).accept(version);
    }
}
