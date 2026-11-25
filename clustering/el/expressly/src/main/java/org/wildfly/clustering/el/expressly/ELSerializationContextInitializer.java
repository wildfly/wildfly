/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.el.expressly;

import org.glassfish.expressly.ExpressionFactoryImpl;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class ELSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public ELSerializationContextInitializer() {
        super(ExpressionFactoryImpl.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new MethodExpressionImplMarshaller());
        context.registerMarshaller(new MethodExpressionLiteralMarshaller());
        context.registerMarshaller(new ValueExpressionImplMarshaller());
        context.registerMarshaller(new ValueExpressionLiteralMarshaller());
    }
}
