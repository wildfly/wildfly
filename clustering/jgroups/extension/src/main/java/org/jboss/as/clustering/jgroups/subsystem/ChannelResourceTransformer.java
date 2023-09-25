/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for channel resources.
 * @author Paul Ferraro
 */
public class ChannelResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    ChannelResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(ChannelResourceDefinition.WILDCARD_PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        new ForkResourceTransformer(this.builder).accept(version);
    }
}
