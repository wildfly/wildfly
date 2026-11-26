/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.ejb.client;

import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * {@link SerializationContextInitializer} service for this module
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class EJBClientSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public EJBClientSerializationContextInitializer() {
        super(SessionID.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(Scalar.BYTE_ARRAY.cast(byte[].class).toMarshaller(SessionID.class, SessionID::getEncodedForm, SessionID::createSessionID));
        context.registerMarshaller(new EJBModuleIdentifierMarshaller());
    }
}
