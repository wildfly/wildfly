/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for auth protocol resources.
 * @author Paul Ferraro
 */
public class AuthProtocolResourceTransformer extends ProtocolResourceTransformer {

    AuthProtocolResourceTransformer(ResourceTransformationDescriptionBuilder builder) {
        super(builder);
    }

    @Override
    public void accept(ModelVersion version) {

        new CipherAuthTokenResourceTransformer(this.builder).accept(version);
        new DigestAuthTokenResourceTransformer(this.builder).accept(version);
        new PlainAuthTokenResourceTransformer(this.builder).accept(version);

        super.accept(version);
    }
}
