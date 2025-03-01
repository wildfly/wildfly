/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.weld.ejb.component.session;

import org.jboss.as.ejb3.component.session.StatelessSerializedProxy;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SessionComponentSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public SessionComponentSerializationContextInitializer() {
        super(StatelessSerializedProxy.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(Scalar.STRING.cast(String.class).toMarshaller(StatelessSerializedProxy.class, StatelessSerializedProxy::getViewName, StatelessSerializedProxy::new));
    }
}
