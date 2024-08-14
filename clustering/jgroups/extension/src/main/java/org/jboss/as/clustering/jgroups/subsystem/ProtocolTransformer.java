/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transforms standard and override protocol resources registered via {@link ProtocolManagementResourceRegistrar}.
 * @author Paul Ferraro
 */
public class ProtocolTransformer implements Consumer<ModelVersion> {

    private final Map<ProtocolResourceDefinitionRegistrar.AuthProtocol, ResourceTransformationDescriptionBuilder> authBuilders = new EnumMap<>(ProtocolResourceDefinitionRegistrar.AuthProtocol.class);
    private final Map<ProtocolResourceDefinitionRegistrar.EncryptProtocol, ResourceTransformationDescriptionBuilder> encryptBuilders = new EnumMap<>(ProtocolResourceDefinitionRegistrar.EncryptProtocol.class);

    ProtocolTransformer(ResourceTransformationDescriptionBuilder parent) {
        for (ProtocolResourceDefinitionRegistrar.AuthProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.AuthProtocol.class)) {
            this.authBuilders.put(protocol, parent.addChildResource(AbstractProtocolResourceDefinitionRegistrar.pathElement(protocol.name())));
        }
        for (ProtocolResourceDefinitionRegistrar.EncryptProtocol protocol : EnumSet.allOf(ProtocolResourceDefinitionRegistrar.EncryptProtocol.class)) {
            this.encryptBuilders.put(protocol, parent.addChildResource(AbstractProtocolResourceDefinitionRegistrar.pathElement(protocol.name())));
        }
    }

    @Override
    public void accept(ModelVersion version) {
        for (ResourceTransformationDescriptionBuilder builder : this.authBuilders.values()) {
            new AuthProtocolResourceTransformer(builder).accept(version);
        }
        for (ResourceTransformationDescriptionBuilder builder : this.encryptBuilders.values()) {
            new EncryptProtocolResourceTransformer(builder).accept(version);
        }
    }
}
