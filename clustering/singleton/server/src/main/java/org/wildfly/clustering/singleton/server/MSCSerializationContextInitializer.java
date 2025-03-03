/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.ExceptionMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.common.function.Functions;

/**
 * Provider of the {@link SerializationContextInitializer} instances for this module.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class MSCSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public MSCSerializationContextInitializer() {
        super(ServiceName.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(Scalar.STRING.cast(String.class).toMarshaller(ServiceName.class, ServiceName::getCanonicalName, Functions.constantSupplier(ServiceName.JBOSS), ServiceName::parse));
        context.registerMarshaller(new ExceptionMarshaller<>(ServiceNotFoundException.class));
        context.registerMarshaller(new ExceptionMarshaller<>(StartException.class));
    }
}
