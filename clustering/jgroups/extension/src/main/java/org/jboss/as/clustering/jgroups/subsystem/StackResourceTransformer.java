/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumSet;
import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transformer for protocol stack resources.
 * @author Paul Ferraro
 */
public class StackResourceTransformer implements Consumer<ModelVersion> {

    private final ResourceTransformationDescriptionBuilder builder;

    StackResourceTransformer(ResourceTransformationDescriptionBuilder parent) {
        this.builder = parent.addChildResource(JGroupsResourceRegistration.STACK.getPathElement());
    }

    @Override
    public void accept(ModelVersion version) {
        for (AuthProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(AuthProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new AuthProtocolResourceTransformer(this.builder, protocol.getPathElement()).accept(version);
        }
        for (EncryptProtocolResourceDefinitionRegistrar.Protocol protocol : EnumSet.allOf(EncryptProtocolResourceDefinitionRegistrar.Protocol.class)) {
            new EncryptProtocolResourceTransformer(this.builder, protocol.getPathElement()).accept(version);
        }

        new SocketTransportResourceTransformer(this.builder).accept(version);
    }
}
