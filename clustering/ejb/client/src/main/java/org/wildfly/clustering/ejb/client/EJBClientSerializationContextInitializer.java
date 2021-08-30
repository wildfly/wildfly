/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.wildfly.clustering.ejb.client;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.jboss.ejb.client.SessionID;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * {@link SerializationContextInitializer} service for this module
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class EJBClientSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public EJBClientSerializationContextInitializer() {
        super("org.jboss.ejb.client.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalScalarMarshaller<>(SessionID.class, Scalar.BYTE_ARRAY.cast(byte[].class), SessionID::getEncodedForm, SessionID::createSessionID));
    }
}
