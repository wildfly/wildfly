/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.fine;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public class FineSessionMetaDataSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new DefaultSessionCreationMetaDataEntryMarshaller());
        context.registerMarshaller(new DefaultSessionAccessMetaDataEntryMarshaller());
    }
}
