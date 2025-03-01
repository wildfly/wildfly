/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;

import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.ListAttribute;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.transform.TransformationContext;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;
import org.jboss.dmr.ModelNode;

/**
 * Transformer for remote cache container resources.
 * @author Paul Ferraro
 */
public class RemoteCacheContainerResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    public RemoteCacheContainerResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(RemoteCacheContainerResourceDefinition.WILDCARD_PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        Map<String, String> legacyModules = new TreeMap<>();
        if (InfinispanSubsystemModel.VERSION_20_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setValueConverter(AttributeConverter.DEFAULT_VALUE, Attribute.PROTOCOL_VERSION.getDefinition())
                    .end();
        }
        if (InfinispanSubsystemModel.VERSION_18_0_0.requiresTransformation(version)) {
            // Convert wildfly-clustering module to the appropriate module alias
            legacyModules.put("org.wildfly.clustering.session.infinispan.remote", "org.wildfly.clustering.web.hotrod");
        }
        if (InfinispanSubsystemModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.ALWAYS, Attribute.TRANSACTION_TIMEOUT.getDefinition())
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Attribute.MARSHALLER.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(Attribute.MARSHALLER.getDefinition().getDefaultValue()), Attribute.MARSHALLER.getDefinition())
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
            }, ListAttribute.MODULES.getDefinition())
            .end();
        }
    }
}
