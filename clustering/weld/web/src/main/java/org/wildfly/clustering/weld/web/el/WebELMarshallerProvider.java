/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.web.el;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import org.jboss.weld.module.web.el.WeldMethodExpression;
import org.jboss.weld.module.web.el.WeldValueExpression;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.reflect.DecoratorMarshaller;

/**
 * @author Paul Ferraro
 */
public enum WebELMarshallerProvider implements ProtoStreamMarshallerProvider {

    METHOD_EXPRESSION(new DecoratorMarshaller<>(MethodExpression.class, WeldMethodExpression::new, null)),
    VALUE_EXPRESSION(new DecoratorMarshaller<>(ValueExpression.class, WeldValueExpression::new, null)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    WebELMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
