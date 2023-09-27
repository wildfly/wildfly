/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.faces.mojarra.facelets.el;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum FaceletsELMarshallerProvider implements ProtoStreamMarshallerProvider {

    CONTEXTUAL_COMPOSITE_VALUE_EXPRESSION(new ContextualCompositeValueExpressionMarshaller()),
    TAG_METHOD_EXPRESSION(new TagMethodExpressionMarshaller()),
    TAG_VALUE_EXPRESSION(new TagValueExpressionMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    FaceletsELMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
