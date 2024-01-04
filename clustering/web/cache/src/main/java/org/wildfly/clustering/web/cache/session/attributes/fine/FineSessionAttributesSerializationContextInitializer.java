/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.attributes.fine;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * {@link SerializationContextInitializer} for this package.
 * @author Paul Ferraro
 */
public class FineSessionAttributesSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new SessionAttributeMapComputeFunctionMarshaller<>());
        context.registerMarshaller(new SessionAttributeMapEntryMarshaller());
    }
}
