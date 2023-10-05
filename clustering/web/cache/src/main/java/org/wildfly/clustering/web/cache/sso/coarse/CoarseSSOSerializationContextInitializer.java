/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.sso.coarse;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.FunctionalScalarMarshaller;
import org.wildfly.clustering.marshalling.protostream.Scalar;

/**
 * @author Paul Ferraro
 */
public class CoarseSSOSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new FunctionalScalarMarshaller<>(SessionFilter.class, Scalar.ANY, SessionFilter::getSession, SessionFilter::new));
    }
}
