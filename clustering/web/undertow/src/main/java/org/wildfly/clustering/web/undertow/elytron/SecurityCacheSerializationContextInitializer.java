/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.clustering.web.undertow.elytron;

import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;
import org.wildfly.security.cache.CachedIdentity;

/**
 * Marshaller registration for the {@link org.wildfly.security.cache} package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class SecurityCacheSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public SecurityCacheSerializationContextInitializer() {
        super(CachedIdentity.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new CachedIdentityMarshaller());
    }
}
