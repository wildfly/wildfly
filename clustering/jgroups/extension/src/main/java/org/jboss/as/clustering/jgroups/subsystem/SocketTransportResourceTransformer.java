/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.DiscardAttributeChecker;
import org.jboss.as.controller.transform.description.RejectAttributeChecker;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for socket transport resources.
 *
 * @author Radoslav Husar
 */
public class SocketTransportResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    SocketTransportResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(SocketTransportResourceDefinitionRegistrar.Transport.TCP.getPathElement());
    }

    @Override
    public void accept(ModelVersion version) {
        if (JGroupsSubsystemModel.VERSION_11_0_0.requiresTransformation(version)) {
            this.builder.getAttributeBuilder()
                    .addRejectCheck(RejectAttributeChecker.DEFINED, SocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT, SocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT)
                    .setDiscard(DiscardAttributeChecker.UNDEFINED, SocketTransportResourceDefinitionRegistrar.CLIENT_SSL_CONTEXT, SocketTransportResourceDefinitionRegistrar.SERVER_SSL_CONTEXT)
                    .end();
        }
    }
}
