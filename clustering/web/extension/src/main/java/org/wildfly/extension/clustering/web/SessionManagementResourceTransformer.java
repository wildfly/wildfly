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
 * @author Paul Ferraro
 */
public class SessionManagementResourceTransformer implements BiConsumer<ModelVersion, ResourceTransformationDescriptionBuilder> {

    @Override
    public void accept(ModelVersion version, ResourceTransformationDescriptionBuilder builder) {
        if (DistributableWebSubsystemModel.VERSION_3_0_0.requiresTransformation(version)) {
            builder.getAttributeBuilder()
                    .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, SessionManagementResourceDefinition.Attribute.MARSHALLER.getName())
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SessionManagementResourceDefinition.Attribute.MARSHALLER.getName())
                    .end();
        }
    }
}
