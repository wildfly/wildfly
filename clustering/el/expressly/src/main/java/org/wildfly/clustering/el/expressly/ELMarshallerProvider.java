/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum ELMarshallerProvider implements ProtoStreamMarshallerProvider {

    METHOD_EXPRESSION_IMPL(new MethodExpressionImplMarshaller()),
    METHOD_EXPRESSION_LITERAL(new MethodExpressionLiteralMarshaller()),
    VALUE_EXPRESSION_IMPL(new ValueExpressionImplMarshaller()),
    VALUE_EXPRESSION_LITERAL(new ValueExpressionLiteralMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ELMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
