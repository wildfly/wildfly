/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.context.flash;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum ContextFlashMarshallerProvider implements ProtoStreamMarshallerProvider {

    SESSION_HELPER(new SessionHelperMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ContextFlashMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
