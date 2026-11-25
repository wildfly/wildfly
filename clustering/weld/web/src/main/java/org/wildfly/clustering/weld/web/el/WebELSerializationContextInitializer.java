/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.web.el;

import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import org.jboss.weld.module.web.el.WeldELResolver;
import org.jboss.weld.module.web.el.WeldMethodExpression;
import org.jboss.weld.module.web.el.WeldValueExpression;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.reflect.DecoratorMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class WebELSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public WebELSerializationContextInitializer() {
        super(WeldELResolver.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new DecoratorMarshaller<>(MethodExpression.class, WeldMethodExpression::new, null));
        context.registerMarshaller(new DecoratorMarshaller<>(ValueExpression.class, WeldValueExpression::new, null));
    }
}
