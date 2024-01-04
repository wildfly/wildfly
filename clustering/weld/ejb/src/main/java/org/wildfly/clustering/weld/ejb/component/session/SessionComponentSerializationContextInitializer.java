/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb.component.session;

import org.infinispan.protostream.SerializationContext;
import org.jboss.as.ejb3.component.session.StatelessSerializedProxy;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * @author Paul Ferraro
 */
public class SessionComponentSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public SessionComponentSerializationContextInitializer() {
        super("org.jboss.as.ejb3.component.session.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalScalarMarshaller<>(StatelessSerializedProxy.class, Scalar.STRING.cast(String.class), StatelessSerializedProxy::getViewName, StatelessSerializedProxy::new));
    }
}
