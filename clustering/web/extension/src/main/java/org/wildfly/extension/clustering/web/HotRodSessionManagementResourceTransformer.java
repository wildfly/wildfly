/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Paul Ferraro
 */
public class HotRodSessionManagementResourceTransformer extends SessionManagementResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder parent;

    public HotRodSessionManagementResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.parent = parent;
    }

    @Override
    public void accept(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = this.parent.addChildResource(HotRodSessionManagementResourceDefinition.WILDCARD_PATH);

        this.accept(version, builder);

        builder.getAttributeBuilder()
                .setDiscard(DiscardAttributeChecker.ALWAYS, HotRodSessionManagementResourceDefinition.Attribute.EXPIRATION_THREAD_POOL_SIZE.getName())
                .end();
    }
}
