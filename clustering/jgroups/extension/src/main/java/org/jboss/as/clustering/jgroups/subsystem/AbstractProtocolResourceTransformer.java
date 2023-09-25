/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Base transformer for protocol/transport/relay resources.
 * @author Paul Ferraro
 */
public class AbstractProtocolResourceTransformer implements Consumer<ModelVersion> {

    final ResourceTransformationDescriptionBuilder builder;

    AbstractProtocolResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void accept(ModelVersion version) {
        // Nothing to transform
    }
}
