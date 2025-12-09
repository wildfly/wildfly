/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.extension.clustering.web;

import java.util.function.BiConsumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for a session management provider.
 * @author Paul Ferraro
 */
public class SessionManagementResourceTransformer implements BiConsumer<ModelVersion, ResourceTransformationDescriptionBuilder> {

    @Override
    public void accept(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        if (DistributableWebSubsystemModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, SessionManagementResourceDefinitionRegistrar.MARSHALLER)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SessionManagementResourceDefinitionRegistrar.MARSHALLER)
                    .end();
        }

        if (DistributableWebSubsystemModel.VERSION_5_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, InfinispanSessionManagementResourceDefinitionRegistrar.IDLE_THRESHOLD)
                    .addRejectCheck(RejectAttributeChecker.DEFINED, InfinispanSessionManagementResourceDefinitionRegistrar.IDLE_THRESHOLD)
                    .end();
        }
    }
}
