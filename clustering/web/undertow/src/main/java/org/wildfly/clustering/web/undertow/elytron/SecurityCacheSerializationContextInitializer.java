/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import org.infinispan.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;

/**
 * Marshaller registration for the {@link org.wildfly.security.cache} package.
 * @author Paul Ferraro
 */
public class SecurityCacheSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public SecurityCacheSerializationContextInitializer() {
        super("org.wildfly.security.cache.proto");
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new CachedIdentityMarshaller());
    }
}
