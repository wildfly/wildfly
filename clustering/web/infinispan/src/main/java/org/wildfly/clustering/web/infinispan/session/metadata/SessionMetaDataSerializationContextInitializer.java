/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.infinispan.session.metadata;

import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.SerializationContextInitializer;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.EnumMarshaller;
import org.wildfly.clustering.web.cache.SessionKeyMarshaller;

/**
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SessionMetaDataSerializationContextInitializer extends AbstractSerializationContextInitializer {

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new SessionKeyMarshaller<>(SessionMetaDataKey.class, SessionMetaDataKey::new));
        context.registerMarshaller(new EnumMarshaller<>(SessionMetaDataKeyFilter.class));
    }
}
