/*
 * Copyright The WildFly Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.wildfly.persistence.jipijapa.hibernate7.cache;

import org.hibernate.cache.internal.CacheKeyImplementation;
import org.kohsuke.MetaInfServices;
import org.wildfly.clustering.marshalling.protostream.AbstractSerializationContextInitializer;
import org.wildfly.clustering.marshalling.protostream.SerializationContext;
import org.wildfly.clustering.marshalling.protostream.SerializationContextInitializer;

/**
 * {@link SerializationContextInitializer} for the {@link org.hibernate.cache.internal} package.
 * @author Paul Ferraro
 */
@MetaInfServices(SerializationContextInitializer.class)
public class HibernateCacheInternalSerializationContextInitializer extends AbstractSerializationContextInitializer {

    public HibernateCacheInternalSerializationContextInitializer() {
        super(CacheKeyImplementation.class.getPackage());
    }

    @Override
    public void registerMarshallers(SerializationContext context) {
        context.registerMarshaller(new BasicCacheKeyImplementationMarshaller());
        context.registerMarshaller(new CacheKeyImplementationMarshaller());
        context.registerMarshaller(new NaturalIdCacheKeyMarshaller());
    }
}
