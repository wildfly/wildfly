/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.PathElement;
import org.jboss.as.controller.security.CredentialReference;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for encrypt protocol resources.
 * @author Paul Ferraro
 */
public class EncryptProtocolResourceTransformer implements Consumer<ModelVersion> {
    private final ResourceTransformationDescriptionBuilder builder;

    EncryptProtocolResourceTransformer(ResourceTransformationDescriptionBuilder parent, PathElement path) {
        this.builder = parent.addChildResource(path);
    }

    @Override
    public void accept(ModelVersion version) {

        if (JGroupsSubsystemModel.VERSION_8_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .addRejectCheck(CredentialReference.REJECT_CREDENTIAL_REFERENCE_WITH_BOTH_STORE_AND_CLEAR_TEXT, EncryptProtocolResourceDefinitionRegistrar.KEY_CREDENTIAL)
                    .end();
        }
    }
}
