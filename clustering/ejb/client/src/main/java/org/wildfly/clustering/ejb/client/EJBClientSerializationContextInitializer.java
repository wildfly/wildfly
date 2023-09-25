/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.ejb.client.SessionID;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * {@link SerializationContextInitializer} service for this module
 * @author Paul Ferraro
 */
public class EJBClientSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public EJBClientSerializationContextInitializer() {
        super("org.jboss.ejb.client.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalScalarMarshaller<>(SessionID.class, Scalar.BYTE_ARRAY.cast(byte[].class), SessionID::getEncodedForm, SessionID::createSessionID));
    }
}
