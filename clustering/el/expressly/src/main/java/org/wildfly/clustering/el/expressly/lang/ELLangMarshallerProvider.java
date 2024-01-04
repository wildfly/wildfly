/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly.lang;

import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;

/**
 * @author Paul Ferraro
 */
public enum ELLangMarshallerProvider implements ProtoStreamMarshallerProvider {

    FUNCTION(new FunctionMarshaller()),
    FUNCTION_MAPPER_IMPL(new FunctionMapperImplMarshaller()),
    VARIABLE_MAPPER_IMPL(new VariableMapperImplMarshaller()),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ELLangMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
