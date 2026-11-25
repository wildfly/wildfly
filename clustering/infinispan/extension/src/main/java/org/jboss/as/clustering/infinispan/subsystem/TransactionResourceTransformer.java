/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.infinispan.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Describes resource transformations for the transaction component of a cache configuration.
 * @author Paul Ferraro
 */
public class TransactionResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    TransactionResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(ComponentResourceRegistration.TRANSACTION.getPathElement());
    }

    @Override
    public void accept(ModelVersion version) {
        if (InfinispanSubsystemModel.VERSION_15_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                   .setDiscard(DiscardAttributeChecker.DEFAULT_VALUE, TransactionResourceDefinitionRegistrar.COMPLETE_TIMEOUT)
                   .addRejectCheck(RejectAttributeChecker.DEFINED, TransactionResourceDefinitionRegistrar.COMPLETE_TIMEOUT)
                   .end();
        }
    }
}
