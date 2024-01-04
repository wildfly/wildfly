/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.cache.session.metadata.coarse;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * @author Paul Ferraro
 */
public class CoarseSessionMetaDataSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new DefaultSessionMetaDataEntryMarshaller());
        context.registerMarshaller(new SessionMetaDataEntryFunctionMarshaller());
    }
}
