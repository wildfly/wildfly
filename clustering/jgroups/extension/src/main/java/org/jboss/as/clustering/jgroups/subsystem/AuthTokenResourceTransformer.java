/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for auth token resources.
 * @author Paul Ferraro
 */
public class AuthTokenResourceTransformer implements Consumer<ModelVersion> {

    final ResourceTransformationDescriptionBuilder builder;

    AuthTokenResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        this.builder = builder;
    }

    @Override
    public void accept(ModelVersion version) {
        if (JGroupsSubsystemModel.VERSION_8_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .addRejectCheck(CredentialReference.REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, AuthTokenResourceDefinitionRegistrar.SHARED_SECRET.getName())
                    .end();
        }
    }
}
