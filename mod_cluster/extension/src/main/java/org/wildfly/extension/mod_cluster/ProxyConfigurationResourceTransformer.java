/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.mod_cluster;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * @author Radoslav Husar
 */
public class ProxyConfigurationResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    public ProxyConfigurationResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(ProxyConfigurationResourceDefinition.WILDCARD_PATH);
    }

    @Override
    public void accept(ModelVersion version) {
        new DynamicLoadProviderResourceTransformer(this.builder).accept(version);
    }
}
