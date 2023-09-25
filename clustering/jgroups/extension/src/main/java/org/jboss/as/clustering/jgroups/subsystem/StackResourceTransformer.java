/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for protocol stack resources.
 * @author Paul Ferraro
 */
public class StackResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    StackResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(StackResourceDefinition.WILDCARD_PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        new ProtocolTransformer(this.builder).accept(version);
    }
}
