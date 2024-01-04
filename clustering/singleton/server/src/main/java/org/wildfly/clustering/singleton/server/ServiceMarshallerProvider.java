/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.singleton.server;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.StartException;
import org.wildfly.clustering.marshalling.protostream.ExceptionMarshaller;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshaller;
import org.wildfly.clustering.marshalling.protostream.ProtoStreamMarshallerProvider;
import org.wildfly.clustering.marshalling.protostream.Scalar;
import org.wildfly.common.function.Functions;

/**
 * Provider of marshallers for the org.jboss.msc.service package.
 * @author Paul Ferraro
 */
public enum ServiceMarshallerProvider implements ProtoStreamMarshallerProvider {
    SERVICE_NAME(new FunctionalScalarMarshaller<>(Scalar.STRING.cast(String.class), Functions.constantSupplier(ServiceName.JBOSS), ServiceName::getCanonicalName, ServiceName::parse)),
    SERVICE_NOT_FOUND_EXCEPTION(new ExceptionMarshaller<>(ServiceNotFoundException.class)),
    START_EXCEPTION(new ExceptionMarshaller<>(StartException.class)),
    ;
    private final ProtoStreamMarshaller<?> marshaller;

    ServiceMarshallerProvider(ProtoStreamMarshaller<?> marshaller) {
        this.marshaller = marshaller;
    }

    @Override
    public ProtoStreamMarshaller<?> getMarshaller() {
        return this.marshaller;
    }
}
