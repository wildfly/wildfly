/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem.remote;

import java.util.function.Consumer;

import org.jboss.as.clustering.infinispan.subsystem.InfinispanSubsystemModel;
import org.jboss.as.clustering.infinispan.subsystem.remote.RemoteCacheContainerResourceDefinition.Attribute;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.AttributeConverter;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

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
        if (InfinispanSubsystemModel.VERSION_16_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setValueConverter(AttributeConverter.DEFAULT_VALUE, Attribute.PROTOCOL_VERSION.getDefinition())
                    .end();
        }
        if (InfinispanSubsystemModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.ALWAYS, Attribute.TRANSACTION_TIMEOUT.getDefinition())
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, Attribute.MARSHALLER.getDefinition())
                    .addRejectCheck(new RejectAttributeChecker.SimpleAcceptAttributeChecker(Attribute.MARSHALLER.getDefinition().getDefaultValue()), Attribute.MARSHALLER.getDefinition())
                    .end();
        }
    }
}
