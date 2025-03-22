/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for the Infinispan session management provider.
 * @author Paul Ferraro
 */
public class InfinispanSessionManagementResourceTransformer extends SessionManagementResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder parent;

    InfinispanSessionManagementResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.parent = parent;
    }

    @Override
    public void accept(ModelVersion version) {
        ResourceTransformationDescriptionBuilder builder = this.parent.addChildResource(SessionManagementResourceRegistration.INFINISPAN.getPathElement());

        this.accept(version, builder);
    }
}
