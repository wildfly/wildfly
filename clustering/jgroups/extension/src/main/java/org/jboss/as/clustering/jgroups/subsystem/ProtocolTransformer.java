/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.clustering.jgroups.subsystem;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.function.Consumer;

import org.jboss.as.clustering.jgroups.subsystem.ProtocolRegistration.AuthProtocol;
import org.jboss.as.clustering.jgroups.subsystem.ProtocolRegistration.EncryptProtocol;
import org.jboss.as.controller.ModelVersion;
import org.jboss.as.controller.transform.description.ResourceTransformationDescriptionBuilder;

/**
 * Transforms standard and override protocol resources registered via {@link ProtocolRegistration}.
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
