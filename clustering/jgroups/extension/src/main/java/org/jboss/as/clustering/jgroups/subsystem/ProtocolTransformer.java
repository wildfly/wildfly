/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.clustering.jgroups.subsystem.ProtocolResourceRegistrar.AuthProtocol;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolResourceRegistrar.EncryptProtocol;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transforms standard and override protocol resources registered via {@link ProtocolResourceRegistrar}.
 * @author Paul Ferraro
 */
public class ProtocolTransformer implements Consumer<ModelVersion> {

    private final Map<AuthProtocol, ResourceTransformationDescriptionBuilder> authBuilders = new EnumMap<>(AuthProtocol.class);
    private final Map<EncryptProtocol, ResourceTransformationDescriptionBuilder> encryptBuilders = new EnumMap<>(EncryptProtocol.class);

    ProtocolTransformer(ResourceTransformationDescriptionBuilder parent) {
        for (AuthProtocol protocol : EnumSet.allOf(AuthProtocol.class)) {
            this.authBuilders.put(protocol, parent.addChildResource(ProtocolResourceDefinition.pathElement(protocol.name())));
        }
        for (EncryptProtocol protocol : EnumSet.allOf(EncryptProtocol.class)) {
            this.encryptBuilders.put(protocol, parent.addChildResource(ProtocolResourceDefinition.pathElement(protocol.name())));
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
